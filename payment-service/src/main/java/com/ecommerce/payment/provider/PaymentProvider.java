package com.ecommerce.payment.provider;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter cho một cổng thanh toán (FR-PAY-004: cô lập theo provider).
 * Thêm VNPay/Stripe = viết thêm một class implements interface này; phần còn lại của service không đổi.
 */
public interface PaymentProvider {

    /** Mã provider: MOCK, VNPAY, STRIPE... */
    String code();

    /** Tạo phiên thanh toán, trả về mã tham chiếu + URL để khách mở trang thanh toán. */
    CheckoutSession createCheckout(UUID orderId, String orderCode, BigDecimal amount, String currency);

    /** Đơn đã hủy: đóng phiên để khách không thanh toán được nữa (nếu provider hỗ trợ). */
    void cancelCheckout(String providerRef);

    /** Hoàn tiền. idempotencyKey giúp gọi lại an toàn: provider không hoàn 2 lần cho cùng khóa. */
    RefundResult refund(String providerRef, BigDecimal amount, String currency, String idempotencyKey);

    /** Xác minh chữ ký + chống replay, rồi đọc nội dung webhook (FR-PAY-007, FR-PAY-008). */
    WebhookEvent verifyWebhook(Map<String, String> headers, String rawBody);

    /** Trạng thái phía provider, dùng cho đối soát (FR-PAY-018). */
    ProviderStatus fetchStatus(String providerRef);

    record CheckoutSession(String providerRef, String checkoutUrl) {
    }

    record RefundResult(boolean succeeded, String providerRefundRef, String failureReason) {
    }

    /** type: PAYMENT_SUCCEEDED | PAYMENT_FAILED */
    record WebhookEvent(String eventId, String type, String providerRef, UUID orderRef, BigDecimal amount,
                        String currency, String failureCode) {
    }

    record ProviderStatus(String status, BigDecimal capturedAmount, BigDecimal refundedAmount) {
    }
}
