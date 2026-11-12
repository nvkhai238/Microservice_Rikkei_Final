package com.ecommerce.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * identity-service: tài khoản, đăng nhập/JWT, hồ sơ, địa chỉ, RBAC.
 * scanBasePackages gồm "com.ecommerce.common" để nạp bean dùng chung
 * (GlobalExceptionHandler, CorrelationIdFilter, Outbox, Audit...).
 */
@SpringBootApplication(scanBasePackages = {"com.ecommerce.identity", "com.ecommerce.common"})
@ConfigurationPropertiesScan
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
