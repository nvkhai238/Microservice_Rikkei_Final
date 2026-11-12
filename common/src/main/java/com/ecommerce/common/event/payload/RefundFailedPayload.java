package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RefundFailedPayload(UUID refundId, UUID paymentId, UUID orderId, BigDecimal amount,
                                  String reason, String idempotencyKey) {
}
