package com.ecommerce.order.order;

import com.ecommerce.common.crypto.Hashing;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.CommonErrorCode;
import com.ecommerce.common.exception.FieldErrorDetail;
import com.ecommerce.order.OrderErrorCode;
import com.ecommerce.order.cart.Cart;
import com.ecommerce.order.cart.CartItem;
import com.ecommerce.order.cart.CartRepository;
import com.ecommerce.order.client.CatalogClient;
import com.ecommerce.order.client.IdentityClient;
import com.ecommerce.order.config.OrderProperties;
import com.ecommerce.order.inventory.InsufficientStockException;
import com.ecommerce.order.inventory.InventoryService;
import com.ecommerce.order.saga.SagaLog;
import com.ecommerce.order.saga.SagaStatus;
import com.ecommerce.order.saga.SagaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Tạo đơn (FR-ORD-001..009, UC-03 bước 1-3, UC-05).
 * <pre>
 *  (ngoài transaction)  kiểm tra Idempotency-Key -> đọc giỏ -> hỏi catalog giá/trạng thái -> hỏi identity địa chỉ
 *  (MỘT transaction)    giành khóa idempotency -> tạo đơn PENDING + snapshot -> GIỮ KHO all-or-nothing
 *                       -> ghi Saga -> ghi OrderCreated vào Outbox -> xóa giỏ
 * </pre>
 * Gọi service khác NGOÀI transaction để không giữ connection DB trong lúc chờ mạng.
 * Đơn, reservation và event nằm cùng transaction => không bao giờ có "đơn mà không giữ kho" hay "event mà không có đơn".
 */
@Slf4j
@Service
public class OrderPlacementService {

    private final CartRepository carts;
    private final OrderRepository orders;
    private final CatalogClient catalog;
    private final IdentityClient identity;
    private final InventoryService inventory;
    private final IdempotencyStore idempotency;
    private final OrderCodeGenerator codes;
    private final OrderEvents orderEvents;
    private final SagaLog sagaLog;
    private final TransactionTemplate tx;
    private final OrderProperties props;
    private final Clock clock;

    public OrderPlacementService(CartRepository carts, OrderRepository orders, CatalogClient catalog,
                                 IdentityClient identity, InventoryService inventory, IdempotencyStore idempotency,
                                 OrderCodeGenerator codes, OrderEvents orderEvents, SagaLog sagaLog,
                                 TransactionTemplate tx, OrderProperties props, Clock clock) {
        this.carts = carts;
        this.orders = orders;
        this.catalog = catalog;
        this.identity = identity;
        this.inventory = inventory;
        this.idempotency = idempotency;
        this.codes = codes;
        this.orderEvents = orderEvents;
        this.sagaLog = sagaLog;
        this.tx = tx;
        this.props = props;
        this.clock = clock;
    }

    /** Kết quả: created=false nghĩa là trả lại đơn cũ do gửi lặp cùng Idempotency-Key. */
    public record Result(OrderDtos.OrderView order, boolean created) {
    }

    public Result place(UUID customerId, String idempotencyKey, OrderDtos.PlaceOrderRequest req, String bearerToken) {
        if (idempotencyKey == null || !idempotencyKey.matches("^[A-Za-z0-9_-]{8,100}$")) {
            throw AppException.validation("Idempotency-Key", "REQUIRED_8_TO_100_CHARS");
        }
        String requestHash = hash(req);
        var replay = replayIfKnown(customerId, idempotencyKey, requestHash);
        if (replay != null) {
            return replay;
        }

        Cart cart = carts.findByCustomerId(customerId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new AppException(OrderErrorCode.CART_EMPTY);
        }
        List<CartItem> cartItems = List.copyOf(cart.getItems());
        Map<UUID, CatalogClient.SkuSnapshot> skus = catalog.lookup(cartItems.stream().map(CartItem::getSkuId).toList());
        List<OrderItem> items = buildItems(cartItems, skus);
        IdentityClient.AddressSnapshot address = identity.getAddress(req.addressId(), bearerToken);
        Money.Totals totals = Money.Totals.of(items, BigDecimal.ZERO, BigDecimal.ZERO);
        if (req.expectedTotal() != null && req.expectedTotal().compareTo(totals.grandTotal()) != 0) {
            throw new AppException(OrderErrorCode.PRICE_CHANGED, "Order total is now " + totals.grandTotal(),
                    List.of(new FieldErrorDetail("expectedTotal", "TOTAL_CHANGED")));
        }

        Instant now = clock.instant();
        // Hàm tạo đơn: gọi lại được để dựng một object MỚI cho nhánh "đơn bị từ chối" (object cũ đã gắn với
        // transaction bị rollback, dùng lại sẽ làm Hibernate rối trạng thái)
        java.util.function.Supplier<Order> newOrder = () -> Order.place(codes.next(), customerId,
                props.order().currency(), req.paymentMethod(), req.note() == null ? null : req.note().trim(),
                toShipping(address), copyItems(items), totals, now.plus(props.inventory().reservationTtl()),
                customerId.toString(), now);
        Order order = newOrder.get();
        List<InventoryService.Line> lines = items.stream()
                .map(i -> new InventoryService.Line(i.getSkuId(), i.getQuantity())).toList();

        try {
            Boolean claimed = tx.execute(status -> {
                if (!idempotency.claim(customerId, idempotencyKey, requestHash, order.getId(), IdempotencyStore.CREATED)) {
                    return false;   // request cùng khóa khác đã thắng trong lúc mình chuẩn bị
                }
                orders.save(order);
                inventory.reserve(order.getId(), lines, order.getReservationExpiresAt());
                sagaLog.start(order.getId(), SagaType.PLACE_ORDER, SagaStatus.AWAITING_PAYMENT, "INVENTORY_RESERVED",
                        "Order " + order.getOrderCode() + " created, stock reserved until " + order.getReservationExpiresAt());
                orderEvents.created(order);
                carts.findByCustomerId(customerId).ifPresent(c -> c.clear(now));
                return true;
            });
            if (!Boolean.TRUE.equals(claimed)) {
                return Objects.requireNonNull(replayIfKnown(customerId, idempotencyKey, requestHash));
            }
            log.info("Order {} placed by {} total {} {}", order.getOrderCode(), customerId, totals.grandTotal(),
                    order.getCurrency());
            return new Result(OrderDtos.OrderView.from(order), true);
        } catch (InsufficientStockException ex) {
            Order rejected = newOrder.get();
            recordRejected(customerId, idempotencyKey, requestHash, rejected, ex);
            throw insufficient(ex, rejected.getOrderCode());
        } catch (DataIntegrityViolationException ex) {
            // Hiếm: trùng mã đơn hoặc tranh chấp khóa idempotency => trả kết quả của request kia nếu có
            var again = replayIfKnown(customerId, idempotencyKey, requestHash);
            if (again != null) {
                return again;
            }
            throw ex;
        }
    }

    /**
     * FR-ORD-009: giữ kho thất bại => đơn được lưu ở trạng thái CANCELLED (không yêu cầu thanh toán),
     * khóa idempotency trỏ vào đơn này => gửi lặp nhận lại đúng kết quả "hết hàng".
     */
    private void recordRejected(UUID customerId, String key, String hash, Order order, InsufficientStockException ex) {
        try {
            tx.executeWithoutResult(status -> {
                if (!idempotency.claim(customerId, key, hash, order.getId(), IdempotencyStore.REJECTED_INSUFFICIENT_STOCK)) {
                    return;
                }
                Instant now = clock.instant();
                order.transitionTo(OrderStatus.CANCELLED, "INVENTORY_INSUFFICIENT", "SYSTEM", now);
                order.changePaymentStatus(PaymentStatus.CANCELLED, now);
                orders.save(order);
                sagaLog.step(order.getId(), SagaType.PLACE_ORDER, SagaStatus.COMPENSATED, "INVENTORY_RESERVATION",
                        "FAILED", "Insufficient stock for " + ex.getShortages().size() + " item(s)");
            });
        } catch (RuntimeException e) {
            log.warn("Could not record rejected order {}: {}", order.getOrderCode(), e.getMessage());
        }
    }

    private Result replayIfKnown(UUID customerId, String key, String requestHash) {
        var entry = idempotency.find(customerId, key).orElse(null);
        if (entry == null) {
            return null;
        }
        if (!entry.requestHash().equals(requestHash)) {
            throw new AppException(CommonErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        Order existing = orders.findById(entry.orderId()).orElseThrow();
        if (IdempotencyStore.REJECTED_INSUFFICIENT_STOCK.equals(entry.outcome())) {
            throw new AppException(CommonErrorCode.INVENTORY_INSUFFICIENT,
                    "Order " + existing.getOrderCode() + " was rejected: insufficient stock");
        }
        return new Result(tx.execute(s -> OrderDtos.OrderView.from(orders.findById(entry.orderId()).orElseThrow())),
                false);
    }

    /** FR-ORD-003, FR-ORD-004: kiểm tra từng dòng còn bán, đúng giới hạn; chụp snapshot giá/tên tại thời điểm mua. */
    private List<OrderItem> buildItems(List<CartItem> cartItems, Map<UUID, CatalogClient.SkuSnapshot> skus) {
        List<FieldErrorDetail> unavailable = new ArrayList<>();
        List<FieldErrorDetail> overLimit = new ArrayList<>();
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem ci = cartItems.get(i);
            CatalogClient.SkuSnapshot sku = skus.get(ci.getSkuId());
            if (sku == null || !sku.sellable()) {
                unavailable.add(new FieldErrorDetail("items[" + i + "].skuId", "SKU_NOT_AVAILABLE"));
                continue;
            }
            int limit = Math.min(sku.maxPerOrder(), props.cart().maxQuantityPerLine());
            if (ci.getQuantity() > limit) {
                overLimit.add(new FieldErrorDetail("items[" + i + "].quantity", "MAX_" + limit));
                continue;
            }
            items.add(new OrderItem(sku.skuId(), sku.skuCode(), sku.productId(), sku.productName(), sku.variantName(),
                    sku.attributes(), sku.imageUrl(), sku.price(), ci.getQuantity()));
        }
        if (!unavailable.isEmpty()) {
            throw new AppException(OrderErrorCode.SKU_NOT_AVAILABLE, OrderErrorCode.SKU_NOT_AVAILABLE.defaultMessage(),
                    unavailable);
        }
        if (!overLimit.isEmpty()) {
            throw new AppException(OrderErrorCode.QUANTITY_LIMIT_EXCEEDED,
                    OrderErrorCode.QUANTITY_LIMIT_EXCEEDED.defaultMessage(), overLimit);
        }
        return items;
    }

    private AppException insufficient(InsufficientStockException ex, String orderCode) {
        List<FieldErrorDetail> details = ex.getShortages().stream()
                .map(s -> new FieldErrorDetail("sku:" + s.skuId(), "AVAILABLE_QUANTITY_EXCEEDED"))
                .toList();
        return new AppException(CommonErrorCode.INVENTORY_INSUFFICIENT,
                "The requested quantity is not available (order " + orderCode + " was not placed)", details);
    }

    private static List<OrderItem> copyItems(List<OrderItem> items) {
        return items.stream().map(i -> new OrderItem(i.getSkuId(), i.getSkuCode(), i.getProductId(),
                i.getProductName(), i.getVariantName(), i.getAttributes(), i.getImageUrl(), i.getUnitPrice(),
                i.getQuantity())).toList();
    }

    private static ShippingAddress toShipping(IdentityClient.AddressSnapshot a) {
        return new ShippingAddress(a.recipientName(), a.phone(), a.line1(), a.ward(), a.district(), a.province(),
                a.postalCode());
    }

    /** "Cùng payload" = cùng địa chỉ, phương thức thanh toán, ghi chú, tổng kỳ vọng. */
    private static String hash(OrderDtos.PlaceOrderRequest r) {
        return Hashing.sha256Hex(r.addressId() + "|" + r.paymentMethod() + "|" + Objects.toString(r.note(), "")
                + "|" + (r.expectedTotal() == null ? "" : r.expectedTotal().stripTrailingZeros().toPlainString()));
    }
}
