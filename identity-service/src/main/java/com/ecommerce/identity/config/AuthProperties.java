package com.ecommerce.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Chính sách xác thực, đọc từ app.auth.* (FR-IAM-003, FR-IAM-009: cấu hình được, không ghi cứng). */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        int bcryptStrength,
        int maxFailedLogins,
        Duration lockDuration,
        boolean emailVerificationRequired,
        Duration emailVerificationTtl,
        Duration passwordResetTtl,
        Password password) {

    public record Password(int minLength, int maxLength) {
    }
}
