package com.ecommerce.order;

import com.ecommerce.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum OrderErrorCode implements ErrorCode {
    CART_EMPTY(HttpStatus.UNPROCESSABLE_ENTITY, "Cart is empty"),
    SKU_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "One or more items are no longer available"),
    QUANTITY_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "Quantity exceeds the allowed limit"),
    PRICE_CHANGED(HttpStatus.CONFLICT, "Prices have changed since the cart was reviewed"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "Order status transition is not allowed"),
    ORDER_NOT_CANCELLABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Order cannot be cancelled in its current status"),
    STOCK_ADJUSTMENT_REJECTED(HttpStatus.CONFLICT, "Stock adjustment would make quantities negative");

    private final HttpStatus status;
    private final String message;

    OrderErrorCode(HttpStatus status, String message) {
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
