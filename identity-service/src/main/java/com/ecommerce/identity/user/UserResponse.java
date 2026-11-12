package com.ecommerce.identity.user;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Thông tin user trả ra ngoài: KHÔNG có passwordHash, số lần sai mật khẩu... */
public record UserResponse(UUID id, String email, String fullName, String phone, UserStatus status,
                           boolean emailVerified, List<String> roles, Instant createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getStatus(),
                u.isEmailVerified(), u.getRoles().stream().sorted().toList(), u.getCreatedAt());
    }
}
