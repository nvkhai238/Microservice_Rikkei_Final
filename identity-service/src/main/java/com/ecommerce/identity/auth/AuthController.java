package com.ecommerce.identity.auth;

import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.identity.user.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final TransactionTemplate transactionTemplate;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ApiResponse.success(authService.register(request), "Account created");
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.TokenResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.TokenResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.success(sessionService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        sessionService.logout(request.refreshToken());
        return ApiResponse.success(null, "Logged out");
    }

    /** FR-IAM-012: người dùng tự thu hồi toàn bộ phiên (vd: nghi lộ mật khẩu). */
    @PostMapping("/logout-all")
    public ApiResponse<Map<String, Integer>> logoutAll() {
        Integer revoked = transactionTemplate.execute(s -> sessionService.revokeAll(CurrentUser.id(), "LOGOUT_ALL"));
        return ApiResponse.success(Map.of("revokedSessions", revoked == null ? 0 : revoked));
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody AuthDtos.TokenRequest request) {
        authService.verifyEmail(request.token());
        return ApiResponse.success(null, "Email verified");
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> resendVerification(@Valid @RequestBody AuthDtos.EmailRequest request) {
        authService.resendVerification(request.email());
        return ApiResponse.success(null, "If the account exists and is not verified, an email has been sent");
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody AuthDtos.EmailRequest request) {
        authService.forgotPassword(request.email());
        return ApiResponse.success(null, "If the account exists, a password reset email has been sent");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null, "Password has been reset");
    }
}
