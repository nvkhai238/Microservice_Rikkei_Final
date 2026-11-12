package com.ecommerce.common.config;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Duration;

/**
 * Cấu hình nền cho mọi service dùng common.
 * <ul>
 *   <li>{@code @AutoConfigurationPackage}: thêm package common vào danh sách Spring Boot quét
 *       @Entity và JpaRepository => OutboxEvent, AuditLog... được nhận diện ở mọi service
 *       mà không cần @EntityScan ở từng service.</li>
 *   <li>{@code @EnableScheduling}: bật @Scheduled (Outbox relay, job hết hạn reservation...).</li>
 *   <li>{@link Clock}: lấy giờ qua bean thay vì Instant.now() => test giả lập thời gian được. Luôn UTC (DR-006).
 *       Làm tròn tới micro giây = độ chính xác của TIMESTAMPTZ trong Postgres, để giá trị trong bộ nhớ
 *       và giá trị đọc lại từ DB luôn bằng nhau.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigurationPackage(basePackages = "com.ecommerce.common")
@EnableScheduling
public class CommonPlatformConfig {

    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
