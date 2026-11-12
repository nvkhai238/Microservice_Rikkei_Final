package com.ecommerce.common.response;

import com.ecommerce.common.web.CorrelationId;

/**
 * Body thành công thống nhất (Phụ lục A.1): code, message, data, correlationId.
 */
public record ApiResponse<T>(String code, String message, T data, String correlationId) {
    private static final String SUCCESS_CODE = "SUCCESS";
    private static final String DEFAULT_MESSAGE = "Request completed successfully";

    public static <T> ApiResponse<T> success(T data) {
        return success(data, DEFAULT_MESSAGE);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(SUCCESS_CODE, message, data, CorrelationId.current());
    }
}
