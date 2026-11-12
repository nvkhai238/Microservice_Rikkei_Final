package com.ecommerce.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** payment-service: payment intent, webhook, hoàn tiền, đối soát (FR-PAY-*). */
@SpringBootApplication(scanBasePackages = {"com.ecommerce.payment", "com.ecommerce.common"})
@ConfigurationPropertiesScan
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
