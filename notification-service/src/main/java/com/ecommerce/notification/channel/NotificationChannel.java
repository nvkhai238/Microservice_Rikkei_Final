package com.ecommerce.notification.channel;

/**
 * Một kênh gửi (FR-NOT-002). Email bắt buộc; SMS/push thêm bằng cách viết class mới implements interface này.
 */
public interface NotificationChannel {

    /** EMAIL | SMS | PUSH */
    String code();

    /**
     * Gửi và trả về mã tham chiếu phía nhà cung cấp (FR-NOT-005).
     * Ném {@link PermanentDeliveryException} nếu lỗi vĩnh viễn (địa chỉ sai...) => không retry.
     */
    String send(String to, String subject, String htmlBody);
}
