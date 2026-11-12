package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderConfirmedPayload(UUID orderId, String orderCode, UUID customerId, String status,
                                    BigDecimal amount, String currency, Instant confirmedAt) {
}
