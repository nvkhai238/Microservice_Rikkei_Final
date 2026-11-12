package com.ecommerce.common.security;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.CommonErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

/*
 * Đọc danh tính người gọi từ JWT ĐÃ ĐƯỢC VERIFY bởi chính service này
 * (không tin header X-User-Id do ai đó gửi tới).
 */
public final class CurrentUser {
    private CurrentUser() {
    }

    public static UUID id() {
        return optionalId().orElseThrow(() -> new AppException(CommonErrorCode.AUTHENTICATION_REQUIRED));
    }

    public static Optional<UUID> optionalId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return Optional.of(UUID.fromString(jwt.getToken().getSubject()));
        }
        return Optional.empty();
    }

    /* Dùng cho audit: id người dùng, hoặc "SYSTEM" nếu là job/consumer chạy nền. */
    public static String actorOrSystem() {
        return optionalId().map(UUID::toString).orElse("SYSTEM");
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    /** Token gốc dạng "Bearer xxx" để chuyển tiếp khi gọi service khác thay mặt người dùng. */
    public static String bearerToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return "Bearer " + jwt.getToken().getTokenValue();
        }
        throw new AppException(CommonErrorCode.AUTHENTICATION_REQUIRED);
    }
}
