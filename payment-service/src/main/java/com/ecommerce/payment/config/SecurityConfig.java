package com.ecommerce.payment.config;

import com.ecommerce.common.security.JsonSecurityErrorHandler;
import com.ecommerce.common.security.Roles;
import com.ecommerce.common.security.SecurityDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JsonSecurityErrorHandler errorHandler)
            throws Exception {
        SecurityDefaults.apply(http, errorHandler)
                .authorizeHttpRequests(auth -> auth
                        // Webhook: không có JWT, được bảo vệ bằng chữ ký HMAC + chống replay (NFR-SEC-013)
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/payments/**").permitAll()
                        // Trang của "cổng thanh toán giả" (thay cho trang VNPay/Stripe thật)
                        .requestMatchers("/api/v1/payments/mock/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/admin/payments/**").hasAnyRole(Roles.FINANCE_STAFF, Roles.ADMIN)
                        .anyRequest().authenticated());
        return http.build();
    }
}
