package com.ecommerce.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** catalog-service: danh mục, thương hiệu, sản phẩm, SKU, hình ảnh, tìm kiếm (FR-CAT-*). */
@SpringBootApplication(scanBasePackages = {"com.ecommerce.catalog", "com.ecommerce.common"})
@ConfigurationPropertiesScan
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
