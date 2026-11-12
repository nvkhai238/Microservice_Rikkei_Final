package com.ecommerce.gateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(boolean enabled, List<Policy> policies) {

    public enum KeyType { IP, USER, USER_OR_IP }

    public record Policy(String name, List<String> paths, List<String> methods, KeyType key, int limit,
                         Duration window) {
    }
}
