package com.ecommerce.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(String from, String locale, int templateVersion, String frontendBaseUrl,
                                     Channels channels) {
    public record Channels(boolean email, boolean sms, boolean push) {
    }
}
