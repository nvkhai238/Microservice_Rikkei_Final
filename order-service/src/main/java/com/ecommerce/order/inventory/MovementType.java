package com.ecommerce.order.inventory;

public enum MovementType {
    /** Nhập kho */
    RECEIPT,
    /** Điều chỉnh tay (kiểm kê, hư hỏng...) */
    ADJUSTMENT,
    /** Giữ chỗ cho đơn */
    RESERVE,
    /** Trả lại phần đang giữ (đơn hủy/hết hạn) */
    RELEASE,
    /** Trừ tồn chính thức khi thanh toán thành công */
    SALE,
    /** Hoàn tồn khi hủy đơn đã trừ kho (FR-INV-014) */
    RESTOCK
}
