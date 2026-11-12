package com.ecommerce.payment.payment;

/** FR-PAY-006 (+ CANCELLED khi đơn bị hủy trước khi khách trả tiền). */
public enum PaymentStatus {
    PENDING, SUCCEEDED, FAILED, EXPIRED, CANCELLED, REFUND_PENDING, PARTIALLY_REFUNDED, REFUNDED;

    /** Đã thu tiền (có thể đã hoàn một phần) */
    public boolean isCaptured() {
        return this == SUCCEEDED || this == REFUND_PENDING || this == PARTIALLY_REFUNDED || this == REFUNDED;
    }
}
