package com.ecommerce.common.event;

/**
 * Tên event = routing key trên RabbitMQ. Bảng hợp đồng đầy đủ: docs/EVENTS.md (SRS mục 5.5).
 */
public final class EventTypes {
    // identity-service
    public static final String USER_REGISTERED = "UserRegistered";
    public static final String EMAIL_VERIFICATION_REQUESTED = "EmailVerificationRequested";
    public static final String PASSWORD_RESET_REQUESTED = "PasswordResetRequested";
    // catalog-service
    public static final String PRODUCT_CHANGED = "ProductChanged";
    // order-service
    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_CONFIRMED = "OrderConfirmed";
    public static final String ORDER_STATUS_CHANGED = "OrderStatusChanged";
    public static final String ORDER_CANCELLED = "OrderCancelled";
    public static final String REFUND_REQUESTED = "RefundRequested";
    public static final String STOCK_LEVEL_CHANGED = "StockLevelChanged";
    // payment-service
    public static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String REFUND_COMPLETED = "RefundCompleted";
    public static final String REFUND_FAILED = "RefundFailed";

    private EventTypes() {
    }
}
