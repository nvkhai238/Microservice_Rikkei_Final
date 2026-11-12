package com.ecommerce.identity.auth;

import com.ecommerce.identity.user.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request/response của luồng xác thực. Mỗi record chỉ có đúng field được phép (DR-012). */
public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password,
            @NotBlank @Size(min = 2, max = 100) String fullName,
            @Pattern(regexp = ValidationPatterns.VN_PHONE) String phone) {
    }

    public record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 128) String password) {
    }

    public record RefreshRequest(@NotBlank @Size(max = 200) String refreshToken) {
    }

    public record TokenRequest(@NotBlank @Size(max = 200) String token) {
    }

    public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(max = 200) String token,
                                       @NotBlank @Size(max = 128) String newPassword) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken,
                                long refreshExpiresIn) {
    }
}
