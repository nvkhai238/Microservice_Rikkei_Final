package com.ecommerce.payment.mock;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.payment.PaymentErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

/**
 * Trang thanh toán của "cổng giả". Khách mở checkoutUrl, bấm "Thanh toán thành công" hoặc "Thất bại".
 * Lưu ý UI-05 / FR-PAY-011: trang "kết quả" mà khách được chuyển về chỉ để HIỂN THỊ;
 * đơn chỉ được xác nhận khi webhook có chữ ký hợp lệ tới payment-service.
 */
@RestController
@RequiredArgsConstructor
public class MockCheckoutController {

    private final MockProviderStore store;
    private final MockWebhookSender webhookSender;

    @GetMapping(value = "/api/v1/payments/mock/checkout/{ref}", produces = MediaType.TEXT_HTML_VALUE)
    public String page(@PathVariable String ref) {
        var s = store.find(ref).orElseThrow(() -> AppException.notFound("Checkout session"));
        String safeRef = HtmlUtils.htmlEscape(ref);
        String body = "OPEN".equals(s.status())
                ? """
                  <form method="post" action="%1$s/pay?outcome=success"><button style="background:#16a34a">Thanh toán thành công</button></form>
                  <form method="post" action="%1$s/pay?outcome=fail"><button style="background:#dc2626">Giả lập thất bại</button></form>
                  """.formatted(safeRef)
                : "<p>Phiên thanh toán đã đóng (trạng thái: " + HtmlUtils.htmlEscape(s.status()) + ").</p>";
        return """
                <!doctype html><html lang="vi"><head><meta charset="utf-8"><title>Mock Pay</title>
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <style>body{font-family:system-ui;max-width:420px;margin:40px auto;padding:0 16px}
                button{color:#fff;border:0;padding:12px 16px;border-radius:8px;width:100%%;margin:6px 0;font-size:16px;cursor:pointer}</style>
                </head><body><h2>Cổng thanh toán giả lập</h2>
                <p>Đơn: <b>%s</b><br>Số tiền: <b>%s %s</b></p>%s
                <p style="color:#666;font-size:13px">Đây là provider MOCK dùng cho môi trường dev/demo.</p></body></html>
                """.formatted(HtmlUtils.htmlEscape(s.orderCode()), s.amount().toPlainString(),
                HtmlUtils.htmlEscape(s.currency()), body);
    }

    /** Khách "trả tiền". Gọi được bằng curl để test tự động: trả JSON. */
    @PostMapping("/api/v1/payments/mock/checkout/{ref}/pay")
    public Map<String, String> pay(@PathVariable String ref, @RequestParam(defaultValue = "success") String outcome) {
        var session = store.find(ref).orElseThrow(() -> AppException.notFound("Checkout session"));
        boolean success = "success".equalsIgnoreCase(outcome);
        if (!store.transitionFromOpen(ref, success ? "PAID" : "FAILED")) {
            throw new AppException(PaymentErrorCode.CHECKOUT_NOT_OPEN);
        }
        webhookSender.sendAsync(session, success);
        return Map.of("status", "PROCESSING",
                "message", "Payment submitted. The order will be updated once the provider confirms it.");
    }
}
