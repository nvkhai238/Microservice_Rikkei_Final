package com.ecommerce.payment.mock;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.payment.PaymentErrorCode;
import com.ecommerce.payment.config.PaymentProperties;
import com.ecommerce.payment.provider.PaymentProvider;
import com.ecommerce.payment.webhook.HmacSignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/**
 * Cổng thanh toán GIẢ LẬP (TBD-02: dùng mock trước để test toàn luồng). Hành xử giống provider thật:
 * có trang thanh toán riêng, báo kết quả bằng webhook có chữ ký HMAC, hoàn tiền idempotent.
 */
@Slf4j
@Component
public class MockPaymentProvider implements PaymentProvider {
    public static final String CODE = "MOCK";
    public static final String SIGNATURE_HEADER = "x-mock-signature";

    private final MockProviderStore store;
    private final PaymentProperties props;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public MockPaymentProvider(MockProviderStore store, PaymentProperties props, JsonMapper jsonMapper, Clock clock) {
        this.store = store;
        this.props = props;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public CheckoutSession createCheckout(UUID orderId, String orderCode, BigDecimal amount, String currency) {
        String ref = "mock_" + UUID.randomUUID().toString().replace("-", "");
        store.create(ref, orderId, orderCode, amount, currency);
        return new CheckoutSession(ref, props.publicBaseUrl() + "/api/v1/payments/mock/checkout/" + ref);
    }

    @Override
    public void cancelCheckout(String providerRef) {
        store.transitionFromOpen(providerRef, "CANCELLED");
    }

    @Override
    public RefundResult refund(String providerRef, BigDecimal amount, String currency, String idempotencyKey) {
        if (props.mock().failRefunds()) {
            return new RefundResult(false, null, "Mock provider configured to fail refunds");
        }
        return store.refund(providerRef, amount, idempotencyKey)
                .map(id -> new RefundResult(true, id, null))
                .orElseGet(() -> new RefundResult(false, null, "Provider rejected refund (state or amount)"));
    }

    @Override
    public WebhookEvent verifyWebhook(Map<String, String> headers, String rawBody) {
        var result = HmacSignature.verify(props.webhook().secret(), headers.get(SIGNATURE_HEADER), rawBody,
                clock.instant(), props.webhook().replayWindow());
        if (result == HmacSignature.Result.INVALID) {
            throw new AppException(PaymentErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
        if (result == HmacSignature.Result.EXPIRED) {
            throw new AppException(PaymentErrorCode.WEBHOOK_REPLAYED);
        }
        JsonNode root = jsonMapper.readTree(rawBody);
        JsonNode data = root.path("data");
        String type = switch (root.path("type").asString()) {
            case "payment.succeeded" -> "PAYMENT_SUCCEEDED";
            case "payment.failed" -> "PAYMENT_FAILED";
            default -> "UNKNOWN";
        };
        return new WebhookEvent(root.path("id").asString(), type, data.path("providerRef").asString(),
                UUID.fromString(data.path("orderRef").asString()), new BigDecimal(data.path("amount").asString()),
                data.path("currency").asString(), data.path("failureCode").asString(null));
    }

    @Override
    public ProviderStatus fetchStatus(String providerRef) {
        return store.find(providerRef)
                .map(s -> new ProviderStatus(s.status(),
                        "OPEN".equals(s.status()) || "FAILED".equals(s.status()) || "CANCELLED".equals(s.status())
                                ? BigDecimal.ZERO : s.amount(), s.refundedAmount()))
                .orElse(new ProviderStatus("NOT_FOUND", BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
