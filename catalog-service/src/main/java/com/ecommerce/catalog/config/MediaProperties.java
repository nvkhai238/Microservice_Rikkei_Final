package com.ecommerce.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(String storageDir, DataSize maxSize, int maxImagesPerProduct, String publicBasePath) {
}
