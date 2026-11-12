package com.ecommerce.identity;

import com.ecommerce.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** Mã lỗi riêng của identity-service (bổ sung cho CommonErrorCode). */
public enum IdentityErrorCode implements ErrorCode {
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Email is already registered"),
    /** FR-IAM-008: cùng một thông báo cho "sai email" và "sai mật khẩu" */
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"),
    INVALID_ONE_TIME_TOKEN(HttpStatus.BAD_REQUEST, "Token is invalid, expired or already used"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Email address has not been verified");

    private final HttpStatus status;
    private final String message;

    IdentityErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus httpStatus() {
        return status;
    }

    @Override
    public String defaultMessage() {
        return message;
    }
}
