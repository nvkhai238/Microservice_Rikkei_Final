package com.ecommerce.identity.auth;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.crypto.Hashing;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.web.ClientIp;
import com.ecommerce.identity.IdentityErrorCode;
import com.ecommerce.identity.config.JwtProperties;
import com.ecommerce.identity.token.AccessTokenIssuer;
import com.ecommerce.identity.token.RefreshToken;
import com.ecommerce.identity.token.RefreshTokenRepository;
import com.ecommerce.identity.user.User;
import com.ecommerce.identity.user.UserRepository;
import com.ecommerce.identity.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Quản lý phiên = refresh token (FR-IAM-011, FR-IAM-012).
 * Access token tự hết hạn sau 15 phút (không thu hồi được), nên "đăng xuất" = thu hồi refresh token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final AccessTokenIssuer accessTokenIssuer;
    private final JwtProperties jwtProperties;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /** Đăng nhập thành công: mở family mới. */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuthDtos.TokenResponse startSession(User user) {
        return issue(user, UUID.randomUUID(), clock.instant()).response();
    }

    /**
     * Refresh = xoay vòng: thu hồi token đang dùng, cấp token mới cùng family.
     * Token đã bị xoay vòng mà còn được gửi lên => có kẻ đang giữ bản sao => thu hồi cả family.
     */
    public AuthDtos.TokenResponse refresh(String rawToken) {
        // Phải COMMIT việc thu hồi family TRƯỚC khi ném lỗi; nếu ném lỗi trong cùng transaction thì rollback mất.
        RefreshOutcome outcome = transactionTemplate.execute(status -> doRefresh(rawToken));
        if (outcome == null || outcome.response() == null) {
            throw new AppException(IdentityErrorCode.INVALID_REFRESH_TOKEN);
        }
        return outcome.response();
    }

    private RefreshOutcome doRefresh(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokens.findByHashForUpdate(Hashing.sha256Hex(rawToken)).orElse(null);
        if (current == null) {
            return RefreshOutcome.rejected();
        }
        if (current.isRevoked()) {
            if (RefreshToken.ROTATED.equals(current.getRevokeReason())) {
                int revoked = refreshTokens.revokeFamily(current.getFamilyId(), "REUSE_DETECTED", now);
                log.warn("Refresh token reuse detected for user {}, revoked {} token(s) in family {}",
                        current.getUserId(), revoked, current.getFamilyId());
                auditService.record(current.getUserId().toString(), "REFRESH_TOKEN_REUSE_DETECTED", "USER",
                        current.getUserId(), null, java.util.Map.of("familyId", current.getFamilyId()), "BLOCKED");
            }
            return RefreshOutcome.rejected();
        }
        if (current.isExpired(now)) {
            return RefreshOutcome.rejected();
        }
        User user = users.findById(current.getUserId()).orElse(null);
        if (user == null || user.getStatus() == UserStatus.DISABLED) {
            refreshTokens.revokeFamily(current.getFamilyId(), "USER_DISABLED", now);
            return RefreshOutcome.rejected();
        }
        Issued issued = issue(user, current.getFamilyId(), now);
        current.rotate(issued.tokenId(), now);
        return new RefreshOutcome(issued.response());
    }

    /** Đăng xuất phiên hiện tại: thu hồi cả family của refresh token. Idempotent: gọi lại vẫn OK. */
    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(Hashing.sha256Hex(rawToken))
                .ifPresent(t -> refreshTokens.revokeFamily(t.getFamilyId(), "LOGOUT", clock.instant()));
    }

    /** Thu hồi MỌI phiên của user (đổi mật khẩu, nghi bị lộ, admin khóa...). */
    @Transactional(propagation = Propagation.MANDATORY)
    public int revokeAll(UUID userId, String reason) {
        return refreshTokens.revokeAllForUser(userId, reason, clock.instant());
    }

    private Issued issue(User user, UUID familyId, Instant now) {
        String rawRefresh = Hashing.randomToken(TOKEN_BYTES);
        RefreshToken token = new RefreshToken(user.getId(), Hashing.sha256Hex(rawRefresh), familyId,
                now.plus(jwtProperties.refreshTokenTtl()), now, ClientIp.current());
        refreshTokens.save(token);
        String access = accessTokenIssuer.issue(user, now);
        return new Issued(token.getId(), new AuthDtos.TokenResponse(access, "Bearer",
                accessTokenIssuer.accessTokenTtlSeconds(), rawRefresh, jwtProperties.refreshTokenTtl().toSeconds()));
    }

    private record Issued(UUID tokenId, AuthDtos.TokenResponse response) {
    }

    private record RefreshOutcome(AuthDtos.TokenResponse response) {
        static RefreshOutcome rejected() {
            return new RefreshOutcome(null);
        }
    }
}
