package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentSucceededPayload(UUID paymentId, UUID orderId, UUID customerId, BigDecimal amount,
                                      String currency, String providerRef, Instant succeededAt) {
}
