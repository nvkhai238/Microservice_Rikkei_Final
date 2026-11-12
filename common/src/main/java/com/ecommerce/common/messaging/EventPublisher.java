package com.ecommerce.common.messaging;

import com.ecommerce.common.event.EventEnvelope;
import com.ecommerce.common.web.CorrelationId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Cách DUY NHẤT để service phát domain event: ghi vào bảng outbox (KHÔNG gửi thẳng RabbitMQ).
 * <p>Vì sao không "save DB xong rồi rabbitTemplate.send()"? (dual-write problem)
 * <ul>
 *   <li>Save xong, app crash trước khi send => DB có đơn nhưng không ai biết => mất event.</li>
 *   <li>Send xong, transaction rollback => mọi người nhận event về một đơn KHÔNG tồn tại.</li>
 * </ul>
 * Ghi event vào cùng transaction với dữ liệu => nguyên tử. {@link OutboxRelay} gửi lên broker sau.
 * <p>{@code Propagation.MANDATORY}: gọi ngoài transaction là lỗi ngay, tránh vô tình mất tính nguyên tử.
 */
@Component
public class EventPublisher {

    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final String producer;

    public EventPublisher(OutboxRepository outboxRepository, JsonMapper jsonMapper, Clock clock,
                          @Value("${spring.application.name}") String producer) {
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.producer = producer;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(String aggregateType, Object aggregateId, String eventType, Object payload) {
        return publish(aggregateType, aggregateId, eventType, 1, payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(String aggregateType, Object aggregateId, String eventType, int version, Object payload) {
        UUID eventId = UUID.randomUUID();
        Instant now = clock.instant();
        String correlationId = CorrelationId.current() != null ? CorrelationId.current() : CorrelationId.newId();
        EventEnvelope<Object> envelope = new EventEnvelope<>(eventId, eventType, version, now, correlationId,
                producer, payload);
        outboxRepository.save(new OutboxEvent(eventId, aggregateType, String.valueOf(aggregateId), eventType,
                version, jsonMapper.writeValueAsString(envelope), correlationId, now));
        return eventId;
    }
}
