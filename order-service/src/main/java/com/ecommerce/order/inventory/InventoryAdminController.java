package com.ecommerce.order.inventory;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.response.PageRequests;
import com.ecommerce.common.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Quản lý kho cho INVENTORY_STAFF / ADMIN (FR-INV-003, mục 2.3). */
@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
public class InventoryAdminController {

    private final InventoryService inventory;
    private final StockRepository stocks;
    private final StockMovementRepository movements;
    private final ReservationRepository reservations;
    private final AuditService auditService;
    private final com.ecommerce.order.config.OrderProperties props;

    @GetMapping("/stocks")
    public ApiResponse<PageResponse<StockView>> list(@RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        PageRequest p = PageRequests.of(page, size, Sort.unsorted());
        List<StockView> items = stocks.page(p.getPageSize(), p.getOffset()).stream().map(StockView::from).toList();
        return ApiResponse.success(PageResponse.of(items, p.getPageNumber(), p.getPageSize(), stocks.count()));
    }

    @GetMapping("/stocks/{skuId}")
    public ApiResponse<StockView> get(@PathVariable UUID skuId) {
        String warehouse = props.inventory().defaultWarehouse();
        var level = stocks.find(skuId, warehouse).orElse(new StockLevel(skuId, warehouse, 0, 0, 0));
        return ApiResponse.success(StockView.from(level));
    }

    @PostMapping("/stocks/{skuId}/receipts")
    public ApiResponse<StockView> receive(@PathVariable UUID skuId, @Valid @RequestBody ReceiptRequest req) {
        return ApiResponse.success(StockView.from(inventory.receive(skuId, req.quantity(), req.reason())));
    }

    @PostMapping("/stocks/{skuId}/adjustments")
    public ApiResponse<StockView> adjust(@PathVariable UUID skuId, @Valid @RequestBody AdjustmentRequest req) {
        return ApiResponse.success(StockView.from(inventory.adjust(skuId, req.delta(), req.reason())));
    }

    @GetMapping("/movements")
    public ApiResponse<PageResponse<MovementView>> movements(@RequestParam(required = false) UUID skuId,
                                                             @RequestParam(required = false) String orderId,
                                                             @RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size) {
        return ApiResponse.success(PageResponse.of(movements.search(skuId, orderId,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), MovementView::from));
    }

    @GetMapping("/reservations")
    public ApiResponse<PageResponse<ReservationView>> reservations(
            @RequestParam(required = false) UUID orderId, @RequestParam(required = false) UUID skuId,
            @RequestParam(required = false) ReservationStatus status, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(PageResponse.of(reservations.search(orderId, skuId, status,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), ReservationView::from));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditService.AuditEntry>> audit(@RequestParam(required = false) String targetId,
                                                                    @RequestParam(required = false) Integer page,
                                                                    @RequestParam(required = false) Integer size) {
        return ApiResponse.success(auditService.search("STOCK", targetId, null,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    public record ReceiptRequest(@NotNull @Min(1) @Max(1_000_000) Long quantity,
                                 @NotBlank @Size(max = 500) String reason) {
    }

    public record AdjustmentRequest(@NotNull @Min(-1_000_000) @Max(1_000_000) Long delta,
                                    @NotBlank @Size(max = 500) String reason) {
    }

    public record StockView(UUID skuId, String warehouseCode, long onHand, long reserved, long available) {
        static StockView from(StockLevel s) {
            return new StockView(s.skuId(), s.warehouseCode(), s.onHand(), s.reserved(), s.available());
        }
    }

    public record MovementView(UUID id, UUID skuId, MovementType type, long quantity, long onHandDelta,
                               long reservedDelta, long onHandAfter, long reservedAfter, String referenceType,
                               String referenceId, String reason, String actor, Instant createdAt) {
        static MovementView from(StockMovement m) {
            return new MovementView(m.getId(), m.getSkuId(), m.getType(), m.getQuantity(), m.getOnHandDelta(),
                    m.getReservedDelta(), m.getOnHandAfter(), m.getReservedAfter(), m.getReferenceType(),
                    m.getReferenceId(), m.getReason(), m.getActor(), m.getCreatedAt());
        }
    }

    public record ReservationView(UUID id, UUID orderId, UUID skuId, int quantity, ReservationStatus status,
                                  Instant expiresAt, Instant createdAt) {
        static ReservationView from(Reservation r) {
            return new ReservationView(r.getId(), r.getOrderId(), r.getSkuId(), r.getQuantity(), r.getStatus(),
                    r.getExpiresAt(), r.getCreatedAt());
        }
    }
}
