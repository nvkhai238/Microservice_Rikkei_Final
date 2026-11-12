package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/** Email/họ tên là PII tối thiểu để notification gửi mail (FR-NOT-004: không query DB của identity). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserRegisteredPayload(UUID userId, String email, String fullName, Instant registeredAt) {
}
