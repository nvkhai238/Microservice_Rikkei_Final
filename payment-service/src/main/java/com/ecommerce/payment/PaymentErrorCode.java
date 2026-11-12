package com.ecommerce.payment;

import com.ecommerce.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PaymentErrorCode implements ErrorCode {
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED, "Invalid webhook signature"),
    WEBHOOK_REPLAYED(HttpStatus.BAD_REQUEST, "Webhook timestamp outside the allowed window"),
    REFUND_EXCEEDS_CAPTURED(HttpStatus.UNPROCESSABLE_ENTITY, "Total refunds would exceed the captured amount"),
    PAYMENT_NOT_REFUNDABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Payment has not been captured"),
    CHECKOUT_NOT_OPEN(HttpStatus.CONFLICT, "Checkout session is no longer open");

    private final HttpStatus status;
    private final String message;

    PaymentErrorCode(HttpStatus status, String message) {
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
