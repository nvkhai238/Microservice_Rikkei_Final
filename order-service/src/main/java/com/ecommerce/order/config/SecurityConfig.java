package com.ecommerce.order.config;

import com.ecommerce.common.security.JsonSecurityErrorHandler;
import com.ecommerce.common.security.Roles;
import com.ecommerce.common.security.SecurityDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/admin/inventory/**").hasAnyRole(Roles.INVENTORY_STAFF, Roles.ADMIN)
                        .requestMatchers("/api/v1/admin/orders/**").hasAnyRole(Roles.ORDER_STAFF, Roles.ADMIN)
                        // A-01, FR-ORD-001: phải đăng nhập mới có giỏ hàng / đặt hàng
                        .requestMatchers("/api/v1/cart/**", "/api/v1/orders/**").authenticated()
                        .anyRequest().authenticated());
        return http.build();
    }
}
