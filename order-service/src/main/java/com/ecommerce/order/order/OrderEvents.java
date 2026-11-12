package com.ecommerce.order.order;

import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.payload.OrderCancelledPayload;
import com.ecommerce.common.event.payload.OrderConfirmedPayload;
import com.ecommerce.common.event.payload.OrderCreatedPayload;
import com.ecommerce.common.event.payload.OrderStatusChangedPayload;
import com.ecommerce.common.event.payload.RefundRequestedPayload;
import com.ecommerce.common.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Tập trung việc phát domain event của đơn (luôn qua Outbox - FR-ORD-008, FR-ORD-015). */
@Component
@RequiredArgsConstructor
public class OrderEvents {
    private static final String AGGREGATE = "Order";

    private final EventPublisher events;

    public void created(Order o) {
        events.publish(AGGREGATE, o.getId(), EventTypes.ORDER_CREATED, new OrderCreatedPayload(
                o.getId(), o.getOrderCode(), o.getCustomerId(), o.getGrandTotal(), o.getCurrency(),
                o.getPaymentMethod(), o.getReservationExpiresAt(),
                o.getItems().stream().map(i -> new OrderCreatedPayload.Item(i.getSkuCode(), i.getProductName(),
                        i.getVariantName(), i.getQuantity(), i.getUnitPrice(), i.getLineTotal())).toList()));
    }

    public void statusChanged(Order o, OrderStatus from, String reason, Instant at) {
        events.publish(AGGREGATE, o.getId(), EventTypes.ORDER_STATUS_CHANGED, new OrderStatusChangedPayload(
                o.getId(), o.getOrderCode(), o.getCustomerId(), from.name(), o.getStatus().name(), reason, at));
    }

    public void confirmed(Order o, Instant at) {
        events.publish(AGGREGATE, o.getId(), EventTypes.ORDER_CONFIRMED, new OrderConfirmedPayload(
                o.getId(), o.getOrderCode(), o.getCustomerId(), o.getStatus().name(), o.getGrandTotal(),
                o.getCurrency(), at));
    }

    public void cancelled(Order o, boolean refundRequired, Instant at) {
        events.publish(AGGREGATE, o.getId(), EventTypes.ORDER_CANCELLED, new OrderCancelledPayload(
                o.getId(), o.getOrderCode(), o.getCustomerId(), o.getCancelReason(), refundRequired,
                o.getGrandTotal(), o.getCurrency(), at));
    }

    /** Lệnh bù: yêu cầu hoàn toàn bộ số đã thu. Khóa idempotency cố định theo đơn => không hoàn 2 lần. */
    public void refundRequested(Order o, String reason) {
        events.publish(AGGREGATE, o.getId(), EventTypes.REFUND_REQUESTED, new RefundRequestedPayload(
                o.getId(), o.getOrderCode(), o.getCustomerId(), o.getGrandTotal(), o.getCurrency(), reason,
                refundKey(o)));
    }

    public static String refundKey(Order o) {
        return "order-cancel:" + o.getId();
    }
}
