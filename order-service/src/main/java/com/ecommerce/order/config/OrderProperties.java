package com.ecommerce.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public record OrderProperties(Inventory inventory, Cart cart, Order order, Clients clients) {

    public record Inventory(Duration reservationTtl, String defaultWarehouse, Duration expiryJobInterval,
                            int expiryBatchSize) {
    }

    public record Cart(int maxLines, int maxQuantityPerLine) {
    }

    public record Order(String currency) {
    }

    public record Clients(String catalogBaseUrl, String identityBaseUrl, Duration connectTimeout,
                          Duration readTimeout) {
    }
}
