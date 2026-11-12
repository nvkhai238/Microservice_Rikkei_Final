package com.ecommerce.order.order;

import com.ecommerce.common.persistence.AssignedIdEntity;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.order.OrderErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate đơn hàng. Đổi trạng thái CHỈ qua {@link #transitionTo}: kiểm tra state machine + ghi lịch sử.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "order_code", nullable = false)
    private String orderCode;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;
    @Column(name = "payment_id")
    private UUID paymentId;
    @Column(nullable = false)
    private String currency;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;
    @Column(name = "discount_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountTotal;
    @Column(name = "tax_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxTotal;
    @Column(name = "shipping_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingFee;
    @Column(name = "grand_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal;
    private String note;
    @Embedded
    private ShippingAddress shippingAddress;
    @Column(name = "reservation_expires_at", nullable = false)
    private Instant reservationExpiresAt;
    @Column(name = "cancel_requested_at")
    private Instant cancelRequestedAt;
    @Column(name = "cancel_reason")
    private String cancelReason;
    @Column(name = "cancelled_by")
    private String cancelledBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    @OrderBy("createdAt ASC")
    private List<OrderStatusHistory> history = new ArrayList<>();

    public static Order place(String orderCode, UUID customerId, String currency, String paymentMethod, String note,
                              ShippingAddress address, List<OrderItem> items, Money.Totals totals,
                              Instant reservationExpiresAt, String actor, Instant now) {
        Order o = new Order();
        o.id = UUID.randomUUID();
        o.orderCode = orderCode;
        o.customerId = customerId;
        o.currency = currency;
        o.paymentMethod = paymentMethod;
        o.note = note;
        o.shippingAddress = address;
        o.items.addAll(items);
        o.subtotal = totals.subtotal();
        o.discountTotal = totals.discountTotal();
        o.taxTotal = totals.taxTotal();
        o.shippingFee = totals.shippingFee();
        o.grandTotal = totals.grandTotal();
        o.reservationExpiresAt = reservationExpiresAt;
        o.status = OrderStatus.PENDING;
        o.paymentStatus = PaymentStatus.AWAITING_PAYMENT;
        o.createdAt = now;
        o.updatedAt = now;
        o.history.add(new OrderStatusHistory(null, OrderStatus.PENDING, "Order placed", actor, now));
        return o;
    }

    /** Đổi trạng thái có kiểm tra (FR-ORD-010) và ghi lịch sử (FR-ORD-011). */
    public OrderStatus transitionTo(OrderStatus next, String reason, String actor, Instant now) {
        if (!status.canTransitionTo(next)) {
            throw new AppException(OrderErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot change order status from " + status + " to " + next);
        }
        OrderStatus previous = status;
        status = next;
        updatedAt = now;
        history.add(new OrderStatusHistory(previous, next, reason, actor, now));
        if (next == OrderStatus.CANCELLED) {
            cancelReason = cancelReason == null ? reason : cancelReason;
            cancelledBy = cancelledBy == null ? actor : cancelledBy;
        }
        return previous;
    }

    public void markPaid(UUID paymentId, Instant now) {
        this.paymentId = paymentId;
        this.paymentStatus = PaymentStatus.PAID;
        this.updatedAt = now;
    }

    public void changePaymentStatus(PaymentStatus paymentStatus, Instant now) {
        this.paymentStatus = paymentStatus;
        this.updatedAt = now;
    }

    public void requestCancellation(String reason, String actor, Instant now) {
        this.cancelRequestedAt = now;
        this.cancelReason = reason;
        this.cancelledBy = actor;
        this.updatedAt = now;
    }

    public boolean isCancellationRequested() {
        return cancelRequestedAt != null;
    }

    public void attachPayment(UUID paymentId) {
        if (this.paymentId == null) {
            this.paymentId = paymentId;
        }
    }
}
