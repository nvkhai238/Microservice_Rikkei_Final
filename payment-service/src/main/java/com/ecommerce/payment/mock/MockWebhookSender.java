package com.ecommerce.payment.mock;

import com.ecommerce.payment.config.PaymentProperties;
import com.ecommerce.payment.webhook.HmacSignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Giả lập việc provider gọi webhook về hệ thống SAU khi khách trả tiền (bất đồng bộ, có độ trễ).
 * Provider thật gửi lại webhook nếu không nhận được 2xx; ở đây có tùy chọn gửi TRÙNG để kiểm chứng idempotency.
 */
@Slf4j
@Component
public class MockWebhookSender {

    private final PaymentProperties props;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final RestClient restClient;
    private final Executor executor = Executors.newFixedThreadPool(4);

    public MockWebhookSender(PaymentProperties props, JsonMapper jsonMapper, Clock clock, RestClient.Builder builder) {
        this.props = props;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder.clone().requestFactory(factory).build();
    }

    public void sendAsync(MockProviderStore.Session session, boolean succeeded) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("providerRef", session.providerRef());
        data.put("orderRef", session.orderRef().toString());
        data.put("amount", session.amount().toPlainString());
        data.put("currency", session.currency());
        if (!succeeded) {
            data.put("failureCode", "DECLINED");
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("id", "evt_" + UUID.randomUUID().toString().replace("-", ""));
        event.put("type", succeeded ? "payment.succeeded" : "payment.failed");
        event.put("created", clock.instant().getEpochSecond());
        event.put("data", data);
        String body = jsonMapper.writeValueAsString(event);

        long delayMs = props.mock().webhookDelay().toMillis();
        CompletableFuture.runAsync(() -> deliver(body), CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS, executor))
                .thenRunAsync(() -> {
                    if (props.mock().duplicateWebhook()) {
                        deliver(body);   // provider "gửi lại" cùng event id => hệ thống phải bỏ qua bản trùng
                    }
                }, executor);
    }

    private void deliver(String body) {
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                restClient.post().uri(props.mock().webhookUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(MockPaymentProvider.SIGNATURE_HEADER, HmacSignature.header(props.webhook().secret(),
                                clock.instant(), body))
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                log.debug("Mock webhook delivered (attempt {})", attempt);
                return;
            } catch (Exception ex) {
                log.warn("Mock webhook delivery attempt {} failed: {}", attempt, ex.getMessage());
                try {
                    Thread.sleep(1000L * attempt * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
