package com.ecommerce.order.order;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.event.payload.PaymentFailedPayload;
import com.ecommerce.common.event.payload.PaymentSucceededPayload;
import com.ecommerce.common.event.payload.RefundCompletedPayload;
import com.ecommerce.common.event.payload.RefundFailedPayload;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.order.OrderErrorCode;
import com.ecommerce.order.inventory.InventoryService;
import com.ecommerce.order.inventory.ReservationStatus;
import com.ecommerce.order.saga.SagaLog;
import com.ecommerce.order.saga.SagaStatus;
import com.ecommerce.order.saga.SagaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Điều phối Saga của đơn hàng (CON-05, FR-SAGA-*). Mỗi hàm = một bước, chạy trong MỘT transaction cục bộ:
 * đổi trạng thái đơn + kho + ghi Saga + ghi event Outbox cùng commit.
 * <p>Mọi hàm đều IDEMPOTENT và chịu được event sai thứ tự (FR-SAGA-005, FR-SAGA-007): luôn kiểm tra trạng thái
 * hiện tại trước khi làm; nếu đã làm rồi hoặc không còn phù hợp thì bỏ qua (có ghi log).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLifecycleService {

    private final OrderRepository orders;
    private final InventoryService inventory;
    private final OrderEvents events;
    private final SagaLog sagaLog;
    private final AuditService audit;
    private final Clock clock;

    // ===================== Thanh toán thành công (UC-03 bước 6-7) =====================
    @Transactional(propagation = Propagation.MANDATORY)
    public void onPaymentSucceeded(PaymentSucceededPayload p) {
        Order order = lock(p.orderId());
        if (order == null) {
            return;
        }
        Instant now = clock.instant();
        order.attachPayment(p.paymentId());
        switch (order.getStatus()) {
            case PENDING -> {
                // BR-008: số tiền thu phải bằng tổng chốt trên đơn
                if (p.amount().compareTo(order.getGrandTotal()) != 0 || !p.currency().equals(order.getCurrency())) {
                    order.markPaid(p.paymentId(), now);
                    cancelPending(order, "PAYMENT_AMOUNT_MISMATCH", "SYSTEM", now);
                    requestRefund(order, "Payment amount mismatch");
                    sagaLog.fail(order.getId(), SagaType.PLACE_ORDER, "PAYMENT_VERIFICATION",
                            "Paid " + p.amount() + " " + p.currency() + " but order total is " + order.getGrandTotal());
                    return;
                }
                int committed = inventory.commit(order.getId());
                if (committed == 0) {
                    // Không còn reservation ACTIVE (bất thường) => không giao được hàng => hoàn tiền (FR-SAGA-004)
                    order.markPaid(p.paymentId(), now);
                    cancelPending(order, "RESERVATION_MISSING", "SYSTEM", now);
                    requestRefund(order, "Reservation no longer available");
                    sagaLog.fail(order.getId(), SagaType.PLACE_ORDER, "INVENTORY_COMMIT",
                            "No active reservation to commit, refund requested");
                    return;
                }
                order.markPaid(p.paymentId(), now);
                OrderStatus from = order.transitionTo(OrderStatus.CONFIRMED, "Payment succeeded", "SYSTEM", now);
                events.confirmed(order, now);
                events.statusChanged(order, from, "Payment succeeded", now);
                sagaLog.step(order.getId(), SagaType.PLACE_ORDER, SagaStatus.COMPLETED, "ORDER_CONFIRMED", "DONE",
                        "Payment " + p.paymentId() + " captured, " + committed + " reservation(s) committed");
            }
            case CANCELLED -> {
                // Tiền về SAU khi đơn đã hủy (hết hạn giữ kho, khách hủy...) => hoàn tiền tự động (FR-SAGA-004)
                if (order.getPaymentStatus() == PaymentStatus.REFUND_PENDING
                        || order.getPaymentStatus() == PaymentStatus.REFUNDED) {
                    return;
                }
                order.markPaid(p.paymentId(), now);
                requestRefund(order, "Payment received after order was cancelled");
                sagaLog.step(order.getId(), SagaType.PLACE_ORDER, SagaStatus.COMPENSATED, "LATE_PAYMENT_REFUND",
                        "COMPENSATED", "Payment arrived for cancelled order, refund requested");
            }
            default -> log.info("Ignoring duplicate PaymentSucceeded for order {} in status {}",
                    order.getOrderCode(), order.getStatus());
        }
    }

    // ===================== Thanh toán thất bại / hết hạn (UC-04) =====================
    @Transactional(propagation = Propagation.MANDATORY)
    public void onPaymentFailed(PaymentFailedPayload p) {
        Order order = lock(p.orderId());
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return;   // đã xử lý rồi (trùng) hoặc đơn đã đi tiếp => bỏ qua
        }
        Instant now = clock.instant();
        order.attachPayment(p.paymentId());
        PaymentStatus ps = "EXPIRED".equals(p.failureCategory()) ? PaymentStatus.EXPIRED : PaymentStatus.FAILED;
        order.changePaymentStatus(ps, now);
        cancelPending(order, "PAYMENT_" + p.failureCategory(), "SYSTEM", now);
    }

    // ===================== Hoàn tiền xong =====================
    @Transactional(propagation = Propagation.MANDATORY)
    public void onRefundCompleted(RefundCompletedPayload p) {
        Order order = lock(p.orderId());
        if (order == null) {
            return;
        }
        Instant now = clock.instant();
        order.changePaymentStatus(p.fullyRefunded() ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED, now);
        boolean cancelSagaRefund = OrderEvents.refundKey(order).equals(p.idempotencyKey());
        if (order.getStatus() == OrderStatus.CONFIRMED && order.isCancellationRequested() && cancelSagaRefund) {
            // UC-06 bước 5: đủ điều kiện mới chuyển CANCELLED (FR-ORD-019)
            OrderStatus from = order.transitionTo(OrderStatus.CANCELLED, order.getCancelReason(),
                    order.getCancelledBy(), now);
            events.cancelled(order, true, now);
            events.statusChanged(order, from, order.getCancelReason(), now);
            sagaLog.step(order.getId(), SagaType.CANCEL_ORDER, SagaStatus.COMPLETED, "ORDER_CANCELLED", "DONE",
                    "Refund " + p.refundId() + " completed (" + p.amount() + ")");
        } else if (cancelSagaRefund) {
            sagaLog.step(order.getId(), SagaType.PLACE_ORDER, SagaStatus.COMPENSATED, "REFUND_COMPLETED", "DONE",
                    "Refund " + p.refundId() + " completed (" + p.amount() + ")");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void onRefundFailed(RefundFailedPayload p) {
        Order order = lock(p.orderId());
        if (order == null) {
            return;
        }
        order.changePaymentStatus(PaymentStatus.REFUND_FAILED, clock.instant());
        SagaType type = order.getStatus() == OrderStatus.CONFIRMED ? SagaType.CANCEL_ORDER : SagaType.PLACE_ORDER;
        sagaLog.fail(order.getId(), type, "REFUND", "Refund failed: " + p.reason());
    }

    // ===================== Hủy đơn (FR-ORD-016..020, UC-06) =====================
    /**
     * Idempotent (FR-ORD-020): gọi lại khi đã hủy / đang hủy => trả trạng thái hiện tại, không làm gì thêm.
     */
    @Transactional
    public OrderDtos.OrderView cancel(UUID orderId, UUID ownerIdOrNull, String reason) {
        Order order = orders.lockById(orderId)
                .filter(o -> ownerIdOrNull == null || o.getCustomerId().equals(ownerIdOrNull))
                .orElseThrow(() -> AppException.notFound("Order"));
        String actor = CurrentUser.actorOrSystem();
        Instant now = clock.instant();
        if (order.getStatus() == OrderStatus.CANCELLED || order.isCancellationRequested()) {
            return OrderDtos.OrderView.from(order);
        }
        switch (order.getStatus()) {
            case PENDING -> {
                order.changePaymentStatus(PaymentStatus.CANCELLED, now);
                cancelPending(order, reason, actor, now);
            }
            case CONFIRMED -> {
                // Đã thanh toán + đã trừ kho: hoàn tồn ngay, yêu cầu hoàn tiền; CANCELLED khi hoàn tiền xong
                order.requestCancellation(reason, actor, now);
                order.changePaymentStatus(PaymentStatus.REFUND_PENDING, now);
                int restocked = inventory.restock(order.getId(), "Order cancelled: " + reason);
                sagaLog.start(order.getId(), SagaType.CANCEL_ORDER, SagaStatus.AWAITING_REFUND, "STOCK_RESTOCKED",
                        restocked + " reservation(s) restocked");
                events.refundRequested(order, reason);
                sagaLog.step(order.getId(), SagaType.CANCEL_ORDER, SagaStatus.AWAITING_REFUND, "REFUND_REQUESTED",
                        "DONE", "Refund of " + order.getGrandTotal() + " requested");
            }
            default -> throw new AppException(OrderErrorCode.ORDER_NOT_CANCELLABLE,
                    "Order in status " + order.getStatus() + " cannot be cancelled");
        }
        audit.record(actor, "ORDER_CANCEL_REQUESTED", "ORDER", order.getId(), null,
                Map.of("reason", reason, "status", order.getStatus()), "SUCCESS");
        return OrderDtos.OrderView.from(order);
    }

    /** FR-ORD-014: nhân viên chuyển CONFIRMED -> SHIPPING -> COMPLETED. CONFIRMED chỉ do Saga đặt (BR-011). */
    @Transactional
    public OrderDtos.OrderView staffTransition(UUID orderId, OrderStatus target, String reason) {
        if (target != OrderStatus.SHIPPING && target != OrderStatus.COMPLETED) {
            throw new AppException(OrderErrorCode.INVALID_STATUS_TRANSITION,
                    "Staff can only move orders to SHIPPING or COMPLETED; use cancel for cancellation");
        }
        Order order = orders.lockById(orderId).orElseThrow(() -> AppException.notFound("Order"));
        if (order.isCancellationRequested()) {
            throw new AppException(OrderErrorCode.INVALID_STATUS_TRANSITION, "Order cancellation is in progress");
        }
        Instant now = clock.instant();
        String actor = CurrentUser.actorOrSystem();
        OrderStatus from = order.transitionTo(target, reason, actor, now);
        events.statusChanged(order, from, reason, now);
        audit.record("ORDER_STATUS_CHANGED", "ORDER", orderId, Map.of("status", from), Map.of("status", target,
                "reason", reason));
        return OrderDtos.OrderView.from(order);
    }

    // ===================== Hết hạn giữ kho (FR-INV-011, UC-04) =====================
    @Transactional(propagation = Propagation.MANDATORY)
    public void expire(UUID orderId) {
        Order order = orders.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        Instant now = clock.instant();
        order.changePaymentStatus(PaymentStatus.EXPIRED, now);
        cancelPending(order, "RESERVATION_EXPIRED", "SYSTEM", now);
    }

    /** Nhân viên retry Saga lỗi (FR-SAGA-010): hiện hỗ trợ gửi lại yêu cầu hoàn tiền (cùng idempotency key). */
    @Transactional(propagation = Propagation.MANDATORY)
    public String retryRefund(UUID orderId) {
        Order order = lock(orderId);
        if (order == null) {
            throw AppException.notFound("Order");
        }
        if (order.getPaymentStatus() != PaymentStatus.REFUND_FAILED
                && order.getPaymentStatus() != PaymentStatus.REFUND_PENDING) {
            throw AppException.businessRule("NOTHING_TO_RETRY", "Order has no pending or failed refund");
        }
        order.changePaymentStatus(PaymentStatus.REFUND_PENDING, clock.instant());
        events.refundRequested(order, order.getCancelReason() == null ? "Manual retry" : order.getCancelReason());
        return "Refund re-requested for order " + order.getOrderCode();
    }

    /** PENDING -> CANCELLED: trả kho đang giữ + báo cho payment hủy intent + báo khách (FR-SAGA-003). */
    private void cancelPending(Order order, String reason, String actor, Instant now) {
        ReservationStatus releaseAs = "RESERVATION_EXPIRED".equals(reason) ? ReservationStatus.EXPIRED
                : ReservationStatus.RELEASED;
        int released = inventory.release(order.getId(), releaseAs, reason);
        OrderStatus from = order.transitionTo(OrderStatus.CANCELLED, reason, actor, now);
        boolean refundRequired = order.getPaymentStatus() == PaymentStatus.PAID;
        events.cancelled(order, refundRequired, now);
        events.statusChanged(order, from, reason, now);
        sagaLog.step(order.getId(), SagaType.PLACE_ORDER, SagaStatus.COMPENSATED, "ORDER_CANCELLED", "COMPENSATED",
                reason + ": released " + released + " reservation(s)");
    }

    private void requestRefund(Order order, String reason) {
        order.changePaymentStatus(PaymentStatus.REFUND_PENDING, clock.instant());
        events.refundRequested(order, reason);
    }

    private Order lock(UUID orderId) {
        Order order = orders.lockById(orderId).orElse(null);
        if (order == null) {
            log.warn("Event for unknown order {}", orderId);
        }
        return order;
    }
}
