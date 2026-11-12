package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/** failureCategory: DECLINED, EXPIRED, CANCELLED, PROVIDER_ERROR... */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentFailedPayload(UUID paymentId, UUID orderId, UUID customerId, String failureCategory,
                                   Instant failedAt) {
}
