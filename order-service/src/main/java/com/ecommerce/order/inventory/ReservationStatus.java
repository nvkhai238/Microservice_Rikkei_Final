package com.ecommerce.order.inventory;

/**
 * Vòng đời giữ chỗ. Từ ACTIVE chỉ đi được MỘT lần sang COMMITTED/RELEASED/EXPIRED (BR-005).
 * COMMITTED -> RESTOCKED khi hủy đơn đã thanh toán (hoàn tồn).
 */
public enum ReservationStatus {
    ACTIVE, COMMITTED, RELEASED, EXPIRED, RESTOCKED
}
