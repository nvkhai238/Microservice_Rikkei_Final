package com.ecommerce.payment.webhook;

import com.ecommerce.payment.payment.PaymentService;
import com.ecommerce.payment.provider.PaymentProvider;
import com.ecommerce.payment.provider.PaymentProviders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Xử lý webhook (FR-PAY-007..013):
 * <ol>
 *   <li>Xác minh chữ ký + timestamp TRƯỚC KHI đọc nội dung (sai => 401/400, không đụng DB).</li>
 *   <li>Ghi webhook_events với UNIQUE(provider, event id): webhook trùng => bỏ qua, vẫn trả 200
 *       để provider ngừng gửi lại (FR-PAY-009).</li>
 *   <li>Ghi nhận kết quả + event Outbox trong CÙNG transaction. Xử lý rất ngắn => phản hồi nhanh (FR-PAY-013).</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentProviders providers;
    private final PaymentService paymentService;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public String handle(String providerCode, Map<String, String> headers, String rawBody) {
        PaymentProvider provider = providers.get(providerCode.toUpperCase(Locale.ROOT));
        PaymentProvider.WebhookEvent event = provider.verifyWebhook(headers, rawBody);
        return tx.execute(status -> {
            int inserted = jdbc.update("""
                    INSERT INTO webhook_events (id, provider, provider_event_id, event_type, payload, received_at)
                    VALUES (?, ?, ?, ?, ?, now()) ON CONFLICT (provider, provider_event_id) DO NOTHING
                    """, UUID.randomUUID(), provider.code(), event.eventId(), event.type(), rawBody);
            if (inserted == 0) {
                log.info("Duplicate webhook {} ignored", event.eventId());
                return "DUPLICATE";
            }
            String result = switch (event.type()) {
                case "PAYMENT_SUCCEEDED" -> paymentService.recordSuccess(event);
                case "PAYMENT_FAILED" -> paymentService.recordFailure(event);
                default -> "IGNORED_TYPE";
            };
            jdbc.update("UPDATE webhook_events SET result = ?, processed_at = now() WHERE provider = ? AND provider_event_id = ?",
                    result, provider.code(), event.eventId());
            log.info("Webhook {} ({}) -> {}", event.eventId(), event.type(), result);
            return result;
        });
    }
}
