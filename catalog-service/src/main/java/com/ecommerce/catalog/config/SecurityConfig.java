package com.ecommerce.catalog.config;

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
                        // Khách vãng lai xem/tìm sản phẩm không cần đăng nhập (mục 2.3)
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**", "/api/v1/brands/**",
                                "/api/v1/products/**", "/api/v1/media/**").permitAll()
                        // API nội bộ cho order-service. Gateway KHÔNG route /internal/** ra ngoài.
                        // Production: thêm mTLS hoặc network policy để chỉ service nội bộ gọi được.
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/admin/catalog/**").hasAnyRole(Roles.CATALOG_STAFF, Roles.ADMIN)
                        .anyRequest().authenticated());
        return http.build();
    }
}
