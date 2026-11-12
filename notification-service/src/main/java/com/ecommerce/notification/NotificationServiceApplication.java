package com.ecommerce.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** notification-service: nghe event, gửi email (FR-NOT-*). Không có API công khai. */
@SpringBootApplication(scanBasePackages = {"com.ecommerce.notification", "com.ecommerce.common"})
@ConfigurationPropertiesScan
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
