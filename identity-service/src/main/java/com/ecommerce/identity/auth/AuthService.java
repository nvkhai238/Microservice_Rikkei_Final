package com.ecommerce.identity.auth;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.crypto.Hashing;
import com.ecommerce.common.crypto.PayloadCipher;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.payload.AccountTokenPayload;
import com.ecommerce.common.event.payload.UserRegisteredPayload;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.messaging.EventPublisher;
import com.ecommerce.identity.IdentityErrorCode;
import com.ecommerce.identity.config.AuthProperties;
import com.ecommerce.identity.token.OneTimeToken;
import com.ecommerce.identity.token.OneTimeTokenRepository;
import com.ecommerce.identity.user.Normalizer;
import com.ecommerce.identity.user.User;
import com.ecommerce.identity.user.UserRepository;
import com.ecommerce.identity.user.UserResponse;
import com.ecommerce.identity.user.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Đăng ký, đăng nhập, xác minh email, quên/đặt lại mật khẩu (FR-IAM-001..013).
 */
@Slf4j
@Service
public class AuthService {

    private final UserRepository users;
    private final OneTimeTokenRepository oneTimeTokens;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final SessionService sessions;
    private final EventPublisher events;
    private final AuditService audit;
    private final PayloadCipher cipher;
    private final AuthProperties props;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    /**
     * Hash giả để so sánh khi email không tồn tại: thời gian phản hồi "sai email" và "sai mật khẩu"
     * giống nhau => kẻ tấn công không đo thời gian để dò email nào có tài khoản (FR-IAM-008).
     */
    private final String dummyHash;

    public AuthService(UserRepository users, OneTimeTokenRepository oneTimeTokens, PasswordEncoder passwordEncoder,
                       PasswordPolicy passwordPolicy, SessionService sessions, EventPublisher events,
                       AuditService audit, PayloadCipher cipher, AuthProperties props,
                       TransactionTemplate transactionTemplate, Clock clock) {
        this.users = users;
        this.oneTimeTokens = oneTimeTokens;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.sessions = sessions;
        this.events = events;
        this.audit = audit;
        this.cipher = cipher;
        this.props = props;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing-1");
    }

    @Transactional
    public UserResponse register(AuthDtos.RegisterRequest req) {
        String email = Normalizer.email(req.email());
        passwordPolicy.validate(req.password(), email, "password");
        if (users.existsByEmail(email)) {
            throw new AppException(IdentityErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        Instant now = clock.instant();
        User user = User.register(email, passwordEncoder.encode(req.password()), Normalizer.text(req.fullName()),
                Normalizer.phone(req.phone()), props.emailVerificationRequired(), now);
        // saveAndFlush: nếu 2 request đăng ký cùng email chạy song song, unique constraint báo lỗi NGAY ở đây (409)
        users.saveAndFlush(user);

        events.publish("User", user.getId(), EventTypes.USER_REGISTERED,
                new UserRegisteredPayload(user.getId(), user.getEmail(), user.getFullName(), now));
        issueOneTimeToken(user, OneTimeToken.Purpose.EMAIL_VERIFICATION, props.emailVerificationTtl(),
                EventTypes.EMAIL_VERIFICATION_REQUESTED);
        audit.record(user.getId().toString(), "USER_REGISTERED", "USER", user.getId(), null,
                Map.of("email", user.getEmail(), "status", user.getStatus()), "SUCCESS");
        return UserResponse.from(user);
    }

    /**
     * Đăng nhập. Mọi trường hợp thất bại (sai email, sai mật khẩu, đang bị khóa, bị vô hiệu hóa)
     * đều trả CÙNG một lỗi INVALID_CREDENTIALS (FR-IAM-008).
     * Dùng TransactionTemplate để việc tăng đếm số lần sai được COMMIT dù sau đó ném lỗi.
     */
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest req) {
        String email = Normalizer.email(req.email());
        LoginOutcome outcome = transactionTemplate.execute(status -> doLogin(email, req.password()));
        if (outcome == null || outcome.error() != null) {
            throw new AppException(outcome == null ? IdentityErrorCode.INVALID_CREDENTIALS : outcome.error());
        }
        return outcome.tokens();
    }

    private LoginOutcome doLogin(String email, String password) {
        Instant now = clock.instant();
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(password, dummyHash);
            return LoginOutcome.failed(IdentityErrorCode.INVALID_CREDENTIALS);
        }
        if (user.isLocked(now) || user.getStatus() == UserStatus.DISABLED) {
            passwordEncoder.matches(password, dummyHash);
            return LoginOutcome.failed(IdentityErrorCode.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            boolean lockedNow = user.recordFailedLogin(props.maxFailedLogins(), props.lockDuration(), now);
            if (lockedNow) {
                log.warn("User {} locked after too many failed logins", user.getId());
                audit.record(user.getId().toString(), "ACCOUNT_TEMPORARILY_LOCKED", "USER", user.getId(), null,
                        Map.of("lockedUntil", user.getLockedUntil()), "BLOCKED");
            }
            return LoginOutcome.failed(IdentityErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            // Chỉ nói "chưa xác minh" khi mật khẩu ĐÚNG => không lộ thông tin cho người lạ
            return LoginOutcome.failed(IdentityErrorCode.EMAIL_NOT_VERIFIED);
        }
        user.recordSuccessfulLogin(now);
        return new LoginOutcome(sessions.startSession(user), null);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        Instant now = clock.instant();
        OneTimeToken token = consume(rawToken, OneTimeToken.Purpose.EMAIL_VERIFICATION, now);
        User user = users.findById(token.getUserId()).orElseThrow(
                () -> new AppException(IdentityErrorCode.INVALID_ONE_TIME_TOKEN));
        UserStatus before = user.getStatus();
        user.markEmailVerified(now);
        audit.record(user.getId().toString(), "EMAIL_VERIFIED", "USER", user.getId(),
                Map.of("status", before), Map.of("status", user.getStatus()), "SUCCESS");
    }

    /** Luôn trả 202 dù email có tồn tại hay không (không cho dò tài khoản). */
    @Transactional
    public void resendVerification(String rawEmail) {
        users.findByEmail(Normalizer.email(rawEmail))
                .filter(u -> !u.isEmailVerified() && u.getStatus() != UserStatus.DISABLED)
                .ifPresent(u -> issueOneTimeToken(u, OneTimeToken.Purpose.EMAIL_VERIFICATION,
                        props.emailVerificationTtl(), EventTypes.EMAIL_VERIFICATION_REQUESTED));
    }

    /** FR-IAM-013. Luôn trả 202 dù email có tồn tại hay không. */
    @Transactional
    public void forgotPassword(String rawEmail) {
        users.findByEmail(Normalizer.email(rawEmail))
                .filter(u -> u.getStatus() != UserStatus.DISABLED)
                .ifPresent(u -> issueOneTimeToken(u, OneTimeToken.Purpose.PASSWORD_RESET,
                        props.passwordResetTtl(), EventTypes.PASSWORD_RESET_REQUESTED));
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest req) {
        Instant now = clock.instant();
        OneTimeToken token = consume(req.token(), OneTimeToken.Purpose.PASSWORD_RESET, now);
        User user = users.findById(token.getUserId()).orElseThrow(
                () -> new AppException(IdentityErrorCode.INVALID_ONE_TIME_TOKEN));
        passwordPolicy.validate(req.newPassword(), user.getEmail(), "newPassword");
        user.changePassword(passwordEncoder.encode(req.newPassword()), now);
        // Đã nhận được mail reset nghĩa là sở hữu email => coi như email đã xác minh
        user.markEmailVerified(now);
        int revoked = sessions.revokeAll(user.getId(), "PASSWORD_RESET");
        audit.record(user.getId().toString(), "PASSWORD_RESET", "USER", user.getId(), null,
                Map.of("revokedSessions", revoked), "SUCCESS");
    }

    private OneTimeToken consume(String rawToken, OneTimeToken.Purpose purpose, Instant now) {
        OneTimeToken token = oneTimeTokens.findForUpdate(Hashing.sha256Hex(rawToken), purpose)
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new AppException(IdentityErrorCode.INVALID_ONE_TIME_TOKEN));
        token.markUsed(now);
        return token;
    }

    /** Sinh token một lần, lưu HASH, phát event chứa token đã MÃ HÓA để notification gửi mail. */
    private void issueOneTimeToken(User user, OneTimeToken.Purpose purpose, Duration ttl, String eventType) {
        Instant now = clock.instant();
        oneTimeTokens.invalidateActive(user.getId(), purpose, now);
        String raw = Hashing.randomToken(32);
        Instant expiresAt = now.plus(ttl);
        oneTimeTokens.save(new OneTimeToken(user.getId(), purpose, Hashing.sha256Hex(raw), expiresAt, now));
        events.publish("User", user.getId(), eventType, new AccountTokenPayload(user.getId(), user.getEmail(),
                user.getFullName(), cipher.encrypt(raw), expiresAt));
    }

    private record LoginOutcome(AuthDtos.TokenResponse tokens, IdentityErrorCode error) {
        static LoginOutcome failed(IdentityErrorCode error) {
            return new LoginOutcome(null, error);
        }
    }
}
