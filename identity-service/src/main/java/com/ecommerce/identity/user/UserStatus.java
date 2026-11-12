package com.ecommerce.identity.user;

public enum UserStatus {
    /** Đã đăng ký, chờ xác minh email (chỉ khi chính sách bật xác minh) */
    PENDING_VERIFICATION,
    ACTIVE,
    /** Bị quản trị viên vô hiệu hóa */
    DISABLED
}
