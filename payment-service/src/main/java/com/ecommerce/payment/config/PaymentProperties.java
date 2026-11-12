package com.ecommerce.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(String publicBaseUrl, Webhook webhook, Duration refundRecoveryInterval,
                                String reconciliationCron, Duration reconciliationLookback, Mock mock) {

    public record Webhook(String secret, Duration replayWindow) {
    }

    public record Mock(String webhookUrl, Duration webhookDelay, boolean duplicateWebhook, boolean failRefunds) {
    }
}
