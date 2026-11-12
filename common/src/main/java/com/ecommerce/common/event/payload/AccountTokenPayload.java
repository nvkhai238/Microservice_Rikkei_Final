package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Dùng cho EmailVerificationRequested và PasswordResetRequested.
 * Token nằm ở dạng MÃ HÓA (AES-GCM, xem PayloadCipher): event không được chứa bí mật dạng rõ (mục 5.5),
 * nhưng notification vẫn cần token để dựng link trong email.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountTokenPayload(UUID userId, String email, String fullName, String encryptedToken, Instant expiresAt) {
}
