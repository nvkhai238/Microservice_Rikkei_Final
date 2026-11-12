package com.ecommerce.order.saga;

public enum SagaStatus {
    STARTED, AWAITING_PAYMENT, AWAITING_REFUND, COMPLETED, COMPENSATED,
    /** Lỗi không tự phục hồi được => cần nhân viên xem và retry (FR-SAGA-004, FR-SAGA-010) */
    FAILED;

    public boolean isFinished() {
        return this == COMPLETED || this == COMPENSATED;
    }
}
