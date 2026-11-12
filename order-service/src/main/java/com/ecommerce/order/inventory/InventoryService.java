package com.ecommerce.order.inventory;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.payload.StockLevelChangedPayload;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.messaging.EventPublisher;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.order.OrderErrorCode;
import com.ecommerce.order.config.OrderProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nghiệp vụ tồn kho (FR-INV-001..015). Mọi thay đổi số lượng:
 * (1) UPDATE có điều kiện trên stocks, (2) ghi ledger, (3) phát StockLevelChanged qua Outbox.
 * Tất cả trong CÙNG transaction với nghiệp vụ gọi nó (đơn hàng, saga...).
 */
@Slf4j
@Service
public class InventoryService {

    private final StockRepository stocks;
    private final ReservationRepository reservations;
    private final StockMovementRepository movements;
    private final EventPublisher events;
    private final AuditService audit;
    private final Clock clock;
    private final String warehouse;

    public InventoryService(StockRepository stocks, ReservationRepository reservations,
                            StockMovementRepository movements, EventPublisher events, AuditService audit,
                            Clock clock, OrderProperties props) {
        this.stocks = stocks;
        this.reservations = reservations;
        this.movements = movements;
        this.events = events;
        this.audit = audit;
        this.clock = clock;
        this.warehouse = props.inventory().defaultWarehouse();
    }

    public record Line(UUID skuId, int quantity) {
    }

    /**
     * Giữ hàng cho TOÀN BỘ dòng của đơn như một thao tác (FR-INV-006).
     * <ul>
     *   <li>Idempotent theo orderId: đã giữ rồi thì trả lại kết quả cũ, không giữ lần 2 (FR-INV-008).</li>
     *   <li>Sắp xếp theo skuId trước khi khóa: mọi transaction khóa các dòng stocks theo CÙNG thứ tự
     *       => không thể có deadlock kiểu A giữ X chờ Y, B giữ Y chờ X (UC-05: "không có deadlock kéo dài").</li>
     *   <li>Thiếu bất kỳ dòng nào => ném InsufficientStockException => rollback toàn bộ (FR-INV-010).</li>
     * </ul>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Reservation> reserve(UUID orderId, List<Line> lines, Instant expiresAt) {
        List<Reservation> existing = reservations.findByOrderId(orderId);
        if (!existing.isEmpty()) {
            return existing;
        }
        Instant now = clock.instant();
        List<Line> sorted = lines.stream().sorted(Comparator.comparing(Line::skuId)).toList();
        List<InsufficientStockException.Shortage> shortages = new ArrayList<>();
        List<Reservation> created = new ArrayList<>();
        List<StockLevel> changed = new ArrayList<>();
        for (Line line : sorted) {
            var result = stocks.tryReserve(line.skuId(), warehouse, line.quantity());
            if (result.isEmpty()) {
                long available = stocks.find(line.skuId(), warehouse).map(StockLevel::available).orElse(0L);
                shortages.add(new InsufficientStockException.Shortage(line.skuId(), line.quantity(), available));
                continue;   // vẫn kiểm tra các dòng còn lại để báo lỗi đầy đủ một lần
            }
            StockLevel after = result.get();
            changed.add(after);
            created.add(new Reservation(orderId, line.skuId(), warehouse, line.quantity(), expiresAt, now));
            movements.save(new StockMovement(after, MovementType.RESERVE, line.quantity(), 0, line.quantity(),
                    "ORDER", orderId.toString(), null, CurrentUser.actorOrSystem(), now));
        }
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }
        reservations.saveAll(created);
        changed.forEach(this::publishLevel);
        return created;
    }

    /** Thanh toán thành công: ACTIVE -> COMMITTED, trừ tồn chính thức đúng MỘT lần (FR-INV-012, BR-005). */
    @Transactional(propagation = Propagation.MANDATORY)
    public int commit(UUID orderId) {
        Instant now = clock.instant();
        List<Reservation> active = reservations.lockByOrderAndStatus(orderId, ReservationStatus.ACTIVE);
        for (Reservation r : active) {
            StockLevel after = stocks.commit(r.getSkuId(), r.getWarehouseCode(), r.getQuantity())
                    .orElseThrow(() -> new IllegalStateException("Stock inconsistent for reservation " + r.getId()));
            r.moveTo(ReservationStatus.COMMITTED, now);
            movements.save(new StockMovement(after, MovementType.SALE, r.getQuantity(), -r.getQuantity(),
                    -r.getQuantity(), "ORDER", orderId.toString(), "Payment confirmed", "SYSTEM", now));
            publishLevel(after);
        }
        return active.size();
    }

    /**
     * Trả phần đang giữ (đơn hủy trước khi trừ kho, thanh toán thất bại, hết hạn) - FR-INV-011, FR-INV-013.
     * Chỉ xử lý reservation còn ACTIVE => gọi lại lần 2 không cộng trả kho lần nữa.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int release(UUID orderId, ReservationStatus finalStatus, String reason) {
        Instant now = clock.instant();
        List<Reservation> active = reservations.lockByOrderAndStatus(orderId, ReservationStatus.ACTIVE);
        for (Reservation r : active) {
            StockLevel after = stocks.release(r.getSkuId(), r.getWarehouseCode(), r.getQuantity())
                    .orElseThrow(() -> new IllegalStateException("Stock inconsistent for reservation " + r.getId()));
            r.moveTo(finalStatus, now);
            movements.save(new StockMovement(after, MovementType.RELEASE, r.getQuantity(), 0, -r.getQuantity(),
                    "ORDER", orderId.toString(), reason, CurrentUser.actorOrSystem(), now));
            publishLevel(after);
        }
        return active.size();
    }

    /** Hủy đơn ĐÃ trừ kho: hoàn tồn có tham chiếu đơn gốc (FR-INV-014). Chỉ COMMITTED => không hoàn 2 lần. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int restock(UUID orderId, String reason) {
        Instant now = clock.instant();
        List<Reservation> committed = reservations.lockByOrderAndStatus(orderId, ReservationStatus.COMMITTED);
        for (Reservation r : committed) {
            StockLevel after = stocks.addOnHand(r.getSkuId(), r.getWarehouseCode(), r.getQuantity()).orElseThrow();
            r.moveTo(ReservationStatus.RESTOCKED, now);
            movements.save(new StockMovement(after, MovementType.RESTOCK, r.getQuantity(), r.getQuantity(), 0,
                    "ORDER", orderId.toString(), reason, CurrentUser.actorOrSystem(), now));
            publishLevel(after);
        }
        return committed.size();
    }

    /** Nhập kho (FR-INV-003): lý do bắt buộc, có audit. */
    @Transactional
    public StockLevel receive(UUID skuId, long quantity, String reason) {
        stocks.ensureExists(skuId, warehouse);
        StockLevel before = stocks.find(skuId, warehouse).orElseThrow();
        StockLevel after = stocks.addOnHand(skuId, warehouse, quantity).orElseThrow();
        movements.save(new StockMovement(after, MovementType.RECEIPT, quantity, quantity, 0, "MANUAL", null, reason,
                CurrentUser.actorOrSystem(), clock.instant()));
        audit.record("STOCK_RECEIVED", "STOCK", skuId, levelMap(before), Map.of("onHand", after.onHand(),
                "reserved", after.reserved(), "quantity", quantity, "reason", reason));
        publishLevel(after);
        return after;
    }

    /** Điều chỉnh +/- (kiểm kê, hư hỏng). Không được làm tồn âm hoặc nhỏ hơn phần đang giữ (FR-INV-005). */
    @Transactional
    public StockLevel adjust(UUID skuId, long delta, String reason) {
        if (delta == 0) {
            throw AppException.validation("delta", "MUST_NOT_BE_ZERO");
        }
        stocks.ensureExists(skuId, warehouse);
        StockLevel before = stocks.find(skuId, warehouse).orElseThrow();
        StockLevel after = stocks.adjust(skuId, warehouse, delta)
                .orElseThrow(() -> new AppException(OrderErrorCode.STOCK_ADJUSTMENT_REJECTED,
                        "On hand cannot drop below reserved quantity (" + before.reserved() + ")"));
        movements.save(new StockMovement(after, MovementType.ADJUSTMENT, Math.abs(delta), delta, 0, "MANUAL", null,
                reason, CurrentUser.actorOrSystem(), clock.instant()));
        audit.record("STOCK_ADJUSTED", "STOCK", skuId, levelMap(before), Map.of("onHand", after.onHand(),
                "reserved", after.reserved(), "delta", delta, "reason", reason));
        publishLevel(after);
        return after;
    }

    @Transactional(readOnly = true)
    public Map<UUID, Long> availability(List<UUID> skuIds) {
        Map<UUID, Long> result = new java.util.HashMap<>();
        skuIds.forEach(id -> result.put(id, 0L));
        stocks.findAll(skuIds, warehouse).forEach(s -> result.put(s.skuId(), s.available()));
        return result;
    }

    private void publishLevel(StockLevel level) {
        events.publish("Stock", level.skuId(), EventTypes.STOCK_LEVEL_CHANGED,
                new StockLevelChangedPayload(level.skuId(), level.available(), level.version()));
    }

    private static Map<String, Object> levelMap(StockLevel s) {
        return Map.of("onHand", s.onHand(), "reserved", s.reserved());
    }
}
