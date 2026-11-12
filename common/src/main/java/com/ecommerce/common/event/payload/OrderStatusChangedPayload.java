package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderStatusChangedPayload(UUID orderId, String orderCode, UUID customerId, String oldStatus,
                                        String newStatus, String reason, Instant occurredAt) {
}
