package com.ecommerce.catalog;

import com.ecommerce.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CatalogErrorCode implements ErrorCode {
    SLUG_ALREADY_USED(HttpStatus.CONFLICT, "Slug is already in use"),
    SKU_CODE_ALREADY_USED(HttpStatus.CONFLICT, "SKU code is already in use"),
    CATEGORY_CYCLE(HttpStatus.UNPROCESSABLE_ENTITY, "Category hierarchy would contain a cycle"),
    IN_USE(HttpStatus.CONFLICT, "Entity is referenced and cannot be deleted; deactivate it instead"),
    NOT_PUBLISHABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Product cannot be published"),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "Invalid image file");

    private final HttpStatus status;
    private final String message;

    CatalogErrorCode(HttpStatus status, String message) {
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
