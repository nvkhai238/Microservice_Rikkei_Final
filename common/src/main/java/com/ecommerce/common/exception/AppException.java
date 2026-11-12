package com.ecommerce.common.exception;

import java.util.List;

public class AppException extends RuntimeException {
    private final ErrorCode errorCode;
    private final List<FieldErrorDetail> details;

    public AppException(ErrorCode errorCode, String message, List<FieldErrorDetail> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), List.of());
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public static AppException notFound(String what) {
        return new AppException(CommonErrorCode.RESOURCE_NOT_FOUND, what + " not found");
    }

    public static AppException conflict(String message) {
        return new AppException(CommonErrorCode.STATE_CONFLICT, message);
    }

    public static AppException businessRule(String reason, String message) {
        return new AppException(CommonErrorCode.BUSINESS_RULE_VIOLATION, message,
                List.of(new FieldErrorDetail(null, reason)));
    }

    public static AppException validation(String field, String reason) {
        return new AppException(CommonErrorCode.VALIDATION_ERROR, CommonErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(new FieldErrorDetail(field, reason)));
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<FieldErrorDetail> getDetails() {
        return details;
    }
}
