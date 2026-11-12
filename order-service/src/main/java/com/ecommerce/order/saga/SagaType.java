package com.ecommerce.order.saga;

public enum SagaType {
    /** Đặt hàng: giữ kho -> chờ thanh toán -> commit kho + xác nhận (hoặc bù: trả kho + hủy) */
    PLACE_ORDER,
    /** Hủy đơn đã thanh toán: hoàn tồn + hoàn tiền -> CANCELLED (UC-06) */
    CANCEL_ORDER
}
