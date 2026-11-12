package com.ecommerce.order.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OrderDtos {
    private OrderDtos() {
    }

    /**
     * expectedTotal (tùy chọn): tổng tiền khách đã thấy ở bước xác nhận. Lệch với tổng tính lại => 409 PRICE_CHANGED,
     * khách phải xem lại trước khi đặt (FR-CART-007, UI-04).
     */
    public record PlaceOrderRequest(
            @NotNull UUID addressId,
            @NotBlank @Pattern(regexp = "^(MOCK|VNPAY|STRIPE)$") String paymentMethod,
            @Size(max = 500) String note,
            @DecimalMin("0") BigDecimal expectedTotal) {
    }

    public record CancelRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record StatusChangeRequest(@NotNull OrderStatus status, @NotBlank @Size(max = 500) String reason) {
    }

    public record ItemView(UUID skuId, String skuCode, String productName, String variantName,
                           Map<String, String> attributes, String imageUrl, BigDecimal unitPrice, int quantity,
                           BigDecimal discount, BigDecimal lineTotal) {
        static ItemView from(OrderItem i) {
            return new ItemView(i.getSkuId(), i.getSkuCode(), i.getProductName(), i.getVariantName(),
                    i.getAttributes(), i.getImageUrl(), i.getUnitPrice(), i.getQuantity(), i.getDiscount(),
                    i.getLineTotal());
        }
    }

    public record HistoryView(OrderStatus from, OrderStatus to, String reason, String actor, Instant at) {
        static HistoryView from(OrderStatusHistory h) {
            return new HistoryView(h.getFromStatus(), h.getToStatus(), h.getReason(), h.getActor(), h.getCreatedAt());
        }
    }

    public record Totals(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal taxTotal, BigDecimal shippingFee,
                         BigDecimal grandTotal, String currency) {
    }

    /** Chi tiết đơn + dòng thời gian trạng thái (UI-06). */
    public record OrderView(UUID id, String orderCode, UUID customerId, OrderStatus status,
                            PaymentStatus paymentStatus, String paymentMethod, UUID paymentId, Totals totals,
                            ShippingAddress shippingAddress, List<ItemView> items, List<HistoryView> timeline,
                            String note, Instant reservationExpiresAt, boolean cancellationInProgress,
                            String cancelReason, Instant createdAt, Instant updatedAt) {
        public static OrderView from(Order o) {
            return new OrderView(o.getId(), o.getOrderCode(), o.getCustomerId(), o.getStatus(), o.getPaymentStatus(),
                    o.getPaymentMethod(), o.getPaymentId(),
                    new Totals(o.getSubtotal(), o.getDiscountTotal(), o.getTaxTotal(), o.getShippingFee(),
                            o.getGrandTotal(), o.getCurrency()),
                    o.getShippingAddress(), o.getItems().stream().map(ItemView::from).toList(),
                    o.getHistory().stream().map(HistoryView::from).toList(), o.getNote(), o.getReservationExpiresAt(),
                    o.isCancellationRequested() && o.getStatus() != OrderStatus.CANCELLED, o.getCancelReason(),
                    o.getCreatedAt(), o.getUpdatedAt());
        }
    }

    public record OrderSummary(UUID id, String orderCode, OrderStatus status, PaymentStatus paymentStatus,
                               BigDecimal grandTotal, String currency, int itemCount, Instant createdAt) {
        public static OrderSummary from(Order o) {
            return new OrderSummary(o.getId(), o.getOrderCode(), o.getStatus(), o.getPaymentStatus(),
                    o.getGrandTotal(), o.getCurrency(), o.getItems().size(), o.getCreatedAt());
        }
    }
}
