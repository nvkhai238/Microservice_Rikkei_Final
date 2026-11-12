package com.ecommerce.payment.messaging;

import com.ecommerce.common.event.EventEnvelope;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.payload.OrderCancelledPayload;
import com.ecommerce.common.event.payload.OrderCreatedPayload;
import com.ecommerce.common.event.payload.RefundRequestedPayload;
import com.ecommerce.common.messaging.EventMessages;
import com.ecommerce.common.messaging.ProcessedEventStore;
import com.ecommerce.common.web.CorrelationId;
import com.ecommerce.payment.config.MessagingConfig;
import com.ecommerce.payment.payment.PaymentRepository;
import com.ecommerce.payment.payment.PaymentService;
import com.ecommerce.payment.refund.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

/** Consumer idempotent cho event từ order-service (cùng khuôn mẫu với order.PaymentEventListener). */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private static final String CONSUMER = MessagingConfig.ORDER_EVENTS_QUEUE;

    private final EventMessages eventMessages;
    private final ProcessedEventStore processedEvents;
    private final PaymentService paymentService;
    private final RefundService refundService;
    private final PaymentRepository payments;
    private final TransactionTemplate tx;

    @RabbitListener(queues = MessagingConfig.ORDER_EVENTS_QUEUE)
    public void onMessage(Message message) {
        EventEnvelope<JsonNode> envelope = eventMessages.parse(message);
        CorrelationId.runWith(envelope.correlationId(), () -> {
            UUID refundToExecute = tx.execute(status -> {
                if (!processedEvents.markProcessed(envelope.eventId(), CONSUMER)) {
                    return null;
                }
                switch (envelope.eventType()) {
                    case EventTypes.ORDER_CREATED ->
                            paymentService.createForOrder(eventMessages.payload(envelope, OrderCreatedPayload.class));
                    case EventTypes.ORDER_CANCELLED -> paymentService.closeForCancelledOrder(
                            eventMessages.payload(envelope, OrderCancelledPayload.class));
                    case EventTypes.REFUND_REQUESTED -> {
                        RefundRequestedPayload r = eventMessages.payload(envelope, RefundRequestedPayload.class);
                        var payment = payments.findByOrderId(r.orderId()).orElse(null);
                        if (payment == null) {
                            log.error("[ALERT] Refund requested for order {} without payment", r.orderCode());
                            return null;
                        }
                        // Hoàn theo số đã thu thực tế (có thể khác tổng đơn nếu có sai lệch) trừ phần đã hoàn
                        var amount = payment.getCapturedAmount().subtract(payment.getRefundedAmount());
                        return amount.signum() > 0
                                ? refundService.createPending(payment.getId(), amount, r.reason(), r.idempotencyKey(), "SYSTEM")
                                : null;
                    }
                    default -> log.debug("Ignoring {}", envelope.eventType());
                }
                return null;
            });
            if (refundToExecute != null) {
                refundService.execute(refundToExecute);
            }
        });
    }
}
