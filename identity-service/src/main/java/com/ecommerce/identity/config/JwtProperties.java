package com.ecommerce.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String privateKeyPath, String publicKeyPath, Duration accessTokenTtl,
                            Duration refreshTokenTtl) {
}
