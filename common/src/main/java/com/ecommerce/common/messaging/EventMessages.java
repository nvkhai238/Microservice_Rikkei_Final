package com.ecommerce.common.messaging;

import com.ecommerce.common.event.EventEnvelope;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * Đọc message từ RabbitMQ thành EventEnvelope. Payload giữ dạng JsonNode, handler tự chuyển sang record
 * nó cần => consumer bỏ qua được field lạ của version mới (NFR-COMP-002).
 */
@Component
public class EventMessages {
    private static final TypeReference<EventEnvelope<JsonNode>> ENVELOPE_TYPE = new TypeReference<>() {
    };

    private final JsonMapper jsonMapper;

    public EventMessages(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public EventEnvelope<JsonNode> parse(Message message) {
        try {
            return jsonMapper.readValue(new String(message.getBody(), StandardCharsets.UTF_8), ENVELOPE_TYPE);
        } catch (RuntimeException ex) {
            // Message hỏng thì retry bao nhiêu lần cũng hỏng => đẩy thẳng vào DLQ
            throw new AmqpRejectAndDontRequeueException("Malformed event message", ex);
        }
    }

    public <T> T payload(EventEnvelope<JsonNode> envelope, Class<T> type) {
        try {
            return jsonMapper.treeToValue(envelope.payload(), type);
        } catch (RuntimeException ex) {
            throw new AmqpRejectAndDontRequeueException("Malformed payload for " + envelope.eventType(), ex);
        }
    }
}
