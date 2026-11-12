package com.ecommerce.order.messaging;

import com.ecommerce.common.event.EventEnvelope;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.payload.PaymentFailedPayload;
import com.ecommerce.common.event.payload.PaymentSucceededPayload;
import com.ecommerce.common.event.payload.RefundCompletedPayload;
import com.ecommerce.common.event.payload.RefundFailedPayload;
import com.ecommerce.common.messaging.EventMessages;
import com.ecommerce.common.messaging.ProcessedEventStore;
import com.ecommerce.common.web.CorrelationId;
import com.ecommerce.order.config.MessagingConfig;
import com.ecommerce.order.order.OrderLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * Nhận kết quả từ payment-service và đẩy Saga đi tiếp.
 * <p>Khuôn mẫu consumer idempotent (COM-08, FR-SAGA-009) - mọi consumer trong hệ thống đều làm giống vậy:
 * <ol>
 *   <li>Mở transaction.</li>
 *   <li>Ghi "đã xử lý eventId" vào processed_event. Đã có => event trùng => bỏ qua.</li>
 *   <li>Làm nghiệp vụ (đổi trạng thái đơn, kho, ghi event mới vào Outbox).</li>
 *   <li>Commit: bằng chứng đã xử lý và thay đổi nghiệp vụ lưu CÙNG LÚC.</li>
 * </ol>
 * Lỗi => transaction rollback, ném exception => RabbitMQ retry với backoff; quá số lần => DLQ (COM-09).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {
    private static final String CONSUMER = MessagingConfig.PAYMENT_EVENTS_QUEUE;

    private final EventMessages eventMessages;
    private final ProcessedEventStore processedEvents;
    private final OrderLifecycleService lifecycle;
    private final TransactionTemplate tx;

    @RabbitListener(queues = MessagingConfig.PAYMENT_EVENTS_QUEUE)
    public void onMessage(Message message) {
        EventEnvelope<JsonNode> envelope = eventMessages.parse(message);
        CorrelationId.runWith(envelope.correlationId(), () -> tx.executeWithoutResult(status -> {
            if (!processedEvents.markProcessed(envelope.eventId(), CONSUMER)) {
                log.debug("Duplicate event {} ({}) ignored", envelope.eventId(), envelope.eventType());
                return;
            }
            log.info("Handling {} {}", envelope.eventType(), envelope.eventId());
            switch (envelope.eventType()) {
                case EventTypes.PAYMENT_SUCCEEDED ->
                        lifecycle.onPaymentSucceeded(eventMessages.payload(envelope, PaymentSucceededPayload.class));
                case EventTypes.PAYMENT_FAILED ->
                        lifecycle.onPaymentFailed(eventMessages.payload(envelope, PaymentFailedPayload.class));
                case EventTypes.REFUND_COMPLETED ->
                        lifecycle.onRefundCompleted(eventMessages.payload(envelope, RefundCompletedPayload.class));
                case EventTypes.REFUND_FAILED ->
                        lifecycle.onRefundFailed(eventMessages.payload(envelope, RefundFailedPayload.class));
                default -> log.debug("Ignoring event type {}", envelope.eventType());
            }
        }));
    }
}
