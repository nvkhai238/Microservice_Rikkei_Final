package com.ecommerce.payment.payment;

import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.payload.PaymentFailedPayload;
import com.ecommerce.common.event.payload.PaymentSucceededPayload;
import com.ecommerce.common.event.payload.RefundCompletedPayload;
import com.ecommerce.common.event.payload.RefundFailedPayload;
import com.ecommerce.common.messaging.EventPublisher;
import com.ecommerce.payment.refund.Refund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Event của payment luôn đi qua Outbox (FR-PAY-012, FR-PAY-017). */
@Component
@RequiredArgsConstructor
public class PaymentEvents {
    private final EventPublisher events;

    public void succeeded(Payment p, Instant at) {
        events.publish("Payment", p.getId(), EventTypes.PAYMENT_SUCCEEDED, new PaymentSucceededPayload(
                p.getId(), p.getOrderId(), p.getCustomerId(), p.getCapturedAmount(), p.getCurrency(),
                p.getProviderRef(), at));
    }

    public void failed(Payment p, String category, Instant at) {
        events.publish("Payment", p.getId(), EventTypes.PAYMENT_FAILED, new PaymentFailedPayload(
                p.getId(), p.getOrderId(), p.getCustomerId(), category, at));
    }

    public void refundCompleted(Payment p, Refund r) {
        events.publish("Payment", p.getId(), EventTypes.REFUND_COMPLETED, new RefundCompletedPayload(
                r.getId(), p.getId(), p.getOrderId(), p.getCustomerId(), r.getAmount(), p.getRefundedAmount(),
                p.isFullyRefunded(), r.getCurrency(), r.getReason(), r.getIdempotencyKey()));
    }

    public void refundFailed(Payment p, Refund r) {
        events.publish("Payment", p.getId(), EventTypes.REFUND_FAILED, new RefundFailedPayload(
                r.getId(), p.getId(), p.getOrderId(), r.getAmount(), r.getFailureReason(), r.getIdempotencyKey()));
    }
}
