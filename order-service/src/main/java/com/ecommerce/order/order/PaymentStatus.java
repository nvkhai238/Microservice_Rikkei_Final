package com.ecommerce.order.order;

/**
 * Trạng thái thanh toán NHÌN TỪ PHÍA ĐƠN (sub-state, mục 7.1: "hoàn tiền có thể tiếp tục ở payment sub-state").
 * Nguồn sự thật về giao dịch vẫn là payment-service.
 */
public enum PaymentStatus {
    AWAITING_PAYMENT, PAID, FAILED, EXPIRED, CANCELLED, REFUND_PENDING, PARTIALLY_REFUNDED, REFUNDED, REFUND_FAILED
}
