package com.ecommerce.identity.user;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.identity.IdentityErrorCode;
import com.ecommerce.identity.auth.PasswordPolicy;
import com.ecommerce.identity.auth.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/** Hồ sơ của CHÍNH người đang đăng nhập (FR-IAM-014). userId luôn lấy từ token, không lấy từ URL. */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final SessionService sessions;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UserResponse get(UUID userId) {
        return UserResponse.from(load(userId));
    }

    @Transactional
    public UserResponse update(UUID userId, UserDtos.UpdateProfileRequest req) {
        User user = load(userId);
        Map<String, Object> before = snapshot(user);
        user.updateProfile(Normalizer.text(req.fullName()), Normalizer.phone(req.phone()), clock.instant());
        audit.record("PROFILE_UPDATED", "USER", user.getId(), before, snapshot(user));
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(UUID userId, UserDtos.ChangePasswordRequest req) {
        User user = load(userId);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new AppException(IdentityErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        }
        passwordPolicy.validate(req.newPassword(), user.getEmail(), "newPassword");
        user.changePassword(passwordEncoder.encode(req.newPassword()), clock.instant());
        int revoked = sessions.revokeAll(user.getId(), "PASSWORD_CHANGED");
        audit.record("PASSWORD_CHANGED", "USER", user.getId(), null, Map.of("revokedSessions", revoked));
    }

    private User load(UUID userId) {
        return users.findById(userId).orElseThrow(() -> AppException.notFound("User"));
    }

    private static Map<String, Object> snapshot(User u) {
        return Map.of("fullName", u.getFullName(), "phone", u.getPhone() == null ? "" : u.getPhone());
    }
}
