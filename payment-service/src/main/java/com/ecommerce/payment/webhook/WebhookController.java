package com.ecommerce.payment.webhook;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Endpoint provider gọi vào. Nhận body dạng CHUỖI GỐC (không parse trước) vì chữ ký được tính trên từng byte
 * của body: parse rồi serialize lại có thể đổi thứ tự/khoảng trắng => sai chữ ký.
 */
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/api/v1/webhooks/payments/{provider}")
    public Map<String, String> receive(@PathVariable String provider, @RequestBody String rawBody,
                                       HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            headers.put(name.toLowerCase(Locale.ROOT), request.getHeader(name));
        }
        return Map.of("result", webhookService.handle(provider, headers, rawBody));
    }
}
