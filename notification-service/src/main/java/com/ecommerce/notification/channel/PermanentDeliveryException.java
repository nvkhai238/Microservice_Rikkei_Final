package com.ecommerce.notification.channel;

/** Lỗi không thể khắc phục bằng retry (địa chỉ không hợp lệ, template lỗi...). */
public class PermanentDeliveryException extends RuntimeException {
    public PermanentDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
