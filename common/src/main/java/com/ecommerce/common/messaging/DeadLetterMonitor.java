package com.ecommerce.common.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cảnh báo vận hành khi DLQ có message (COM-09, FR-NOT-006). Mỗi service chỉ theo dõi DLQ của các queue
 * MÀ NÓ NGHE (lấy từ các @RabbitListener đang chạy) => không báo trùng giữa các service.
 * Bản demo: log ERROR có tiền tố [ALERT]. Production: nối sang Prometheus/Alertmanager, Slack...
 */
@Slf4j
@Component
public class DeadLetterMonitor {

    private final AmqpAdmin rabbitAdmin;
    private final RabbitListenerEndpointRegistry registry;

    public DeadLetterMonitor(AmqpAdmin rabbitAdmin, RabbitListenerEndpointRegistry registry) {
        this.rabbitAdmin = rabbitAdmin;
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${app.messaging.dlq-check-interval:60s}", initialDelayString = "30s")
    public void check() {
        registry.getListenerContainers().stream()
                .filter(AbstractMessageListenerContainer.class::isInstance)
                .map(AbstractMessageListenerContainer.class::cast)
                .flatMap(c -> java.util.Arrays.stream(c.getQueueNames()))
                .distinct()
                .map(q -> q + EventTopology.DLQ_SUFFIX)
                .forEach(this::checkQueue);
    }

    private void checkQueue(String dlq) {
        try {
            QueueInformation info = rabbitAdmin.getQueueInfo(dlq);
            if (info != null && info.getMessageCount() > 0) {
                log.error("[ALERT] Dead letter queue {} has {} message(s) that need manual handling",
                        dlq, info.getMessageCount());
            }
        } catch (Exception ex) {
            log.warn("Cannot inspect DLQ {}: {}", dlq, ex.getMessage());
        }
    }
}
