package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/** totalRefunded = tổng lũy kế đã hoàn; fullyRefunded = đã hoàn hết số đã thu. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RefundCompletedPayload(UUID refundId, UUID paymentId, UUID orderId, UUID customerId,
                                     BigDecimal amount, BigDecimal totalRefunded, boolean fullyRefunded,
                                     String currency, String reason, String idempotencyKey) {
}
