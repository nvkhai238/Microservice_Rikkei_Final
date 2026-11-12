package com.ecommerce.identity.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {
    }

    /** Chỉ cho sửa họ tên + SĐT. Email, role, status KHÔNG có ở đây => không thể tự nâng quyền (DR-012). */
    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 100) String fullName,
            @Pattern(regexp = ValidationPatterns.VN_PHONE) String phone) {
    }

    public record ChangePasswordRequest(@NotBlank @Size(max = 128) String currentPassword,
                                        @NotBlank @Size(max = 128) String newPassword) {
    }
}
