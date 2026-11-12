package com.ecommerce.common.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Job nền đọc outbox_event PENDING và gửi lên RabbitMQ (FR-SAGA-008).
 * <ul>
 *   <li>Chờ broker xác nhận (publisher confirm) rồi mới đánh dấu PUBLISHED.</li>
 *   <li>Broker lỗi => event vẫn PENDING, lần chạy sau gửi lại. Không mất event.</li>
 *   <li>Có thể gửi TRÙNG (gửi xong, crash trước khi commit PUBLISHED) => at-least-once.
 *       Đó là lý do consumer PHẢI idempotent (xem {@link ProcessedEventStore}).</li>
 * </ul>
 */
@Slf4j
@Component
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final int batchSize;
    private final Duration retention;

    public OutboxRelay(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate,
                       TransactionTemplate transactionTemplate, Clock clock,
                       @Value("${app.outbox.batch-size:100}") int batchSize,
                       @Value("${app.outbox.retention:7d}") Duration retention) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.batchSize = batchSize;
        this.retention = retention;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:500ms}")
    public void relay() {
        // Lặp tới khi hết lô đầy (để xả backlog nhanh), mỗi lô một transaction ngắn
        Integer sent;
        do {
            sent = transactionTemplate.execute(status -> publishBatch());
        } while (sent != null && sent == batchSize);
    }

    private int publishBatch() {
        List<OutboxEvent> batch = outboxRepository.lockNextBatch(batchSize);
        int sent = 0;
        for (OutboxEvent event : batch) {
            try {
                Message message = MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                        .setContentType("application/json")
                        .setContentEncoding("UTF-8")
                        .setMessageId(event.getId().toString())
                        .setType(event.getEventType())
                        .setCorrelationId(event.getCorrelationId())
                        .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                        .build();
                rabbitTemplate.invoke(ops -> {
                    ops.send(EventTopology.EXCHANGE, event.getEventType(), message);
                    ops.waitForConfirmsOrDie(5_000);
                    return null;
                });
                event.markPublished(clock.instant());
                sent++;
            } catch (Exception ex) {
                event.markFailedAttempt(ex.getMessage());
                log.warn("Outbox publish failed for event {} ({}), attempt {}: {}",
                        event.getId(), event.getEventType(), event.getAttempts(), ex.getMessage());
                // Giữ thứ tự: dừng lô này, lần sau thử lại từ event lỗi
                break;
            }
        }
        if (sent > 0) {
            log.debug("Outbox relayed {} event(s)", sent);
        }
        return sent == batch.size() ? sent : -1;
    }

    @Scheduled(cron = "${app.outbox.cleanup-cron:0 0 3 * * *}")
    public void cleanup() {
        Integer deleted = transactionTemplate.execute(s ->
                outboxRepository.deletePublishedBefore(clock.instant().minus(retention)));
        if (deleted != null && deleted > 0) {
            log.info("Outbox cleanup removed {} published event(s)", deleted);
        }
    }
}
