package com.ecommerce.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * "Phong bì" bọc mọi event (COM-07). Consumer đọc metadata để:
 * <ul>
 *   <li>eventId: chống xử lý trùng (COM-08)</li>
 *   <li>eventType + eventVersion: chọn handler, tương thích phiên bản (NFR-COMP-002)</li>
 *   <li>correlationId: nối log xuyên service</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,
        String producer,
        T payload) {
}
