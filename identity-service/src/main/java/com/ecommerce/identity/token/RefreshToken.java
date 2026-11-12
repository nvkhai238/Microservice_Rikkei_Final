package com.ecommerce.identity.token;

import com.ecommerce.common.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token (FR-IAM-011). Mỗi lần đăng nhập tạo một "family" mới.
 * Mỗi lần refresh: token cũ bị thu hồi (ROTATED), token mới cùng family được cấp.
 * Nếu ai đó dùng lại token đã ROTATED => token đã bị đánh cắp => thu hồi CẢ family.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends AssignedIdEntity {

    public static final String ROTATED = "ROTATED";

    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;
    @Column(name = "family_id", nullable = false)
    private UUID familyId;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "revoke_reason")
    private String revokeReason;
    @Column(name = "replaced_by")
    private UUID replacedBy;
    @Column(name = "client_ip")
    private String clientIp;

    public RefreshToken(UUID userId, String tokenHash, UUID familyId, Instant expiresAt, Instant now, String clientIp) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.createdAt = now;
        this.clientIp = clientIp;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void rotate(UUID newTokenId, Instant now) {
        revokedAt = now;
        revokeReason = ROTATED;
        replacedBy = newTokenId;
    }
}
