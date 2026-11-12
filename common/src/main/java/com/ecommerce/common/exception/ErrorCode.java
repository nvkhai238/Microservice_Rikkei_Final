package com.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String code();
    HttpStatus httpStatus();
    String defaultMessage();
}
