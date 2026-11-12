package com.ecommerce.common.exception;

import com.ecommerce.common.web.CorrelationId;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldErrorDetail> details,
        String correlationId) {

    public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldErrorDetail> details) {
        return new ErrorResponse(
                errorCode.code(),
                message,
                details == null ? List.of() : List.copyOf(details),
                CorrelationId.current());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return of(errorCode, message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.defaultMessage(), List.of());
    }
}
