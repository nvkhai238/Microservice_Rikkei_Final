package com.ecommerce.gateway.config;

import com.ecommerce.gateway.error.GatewayErrors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import reactor.core.publisher.Mono;

/**
 * Lớp bảo vệ thứ nhất (FR-GW-003): verify chữ ký + hạn + issuer của JWT trước khi cho request đi tiếp.
 * Route public (xem sản phẩm, đăng nhập, webhook...) không cần token.
 * Service phía sau VẪN tự verify lần nữa (không tin mạng nội bộ - zero trust).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] STAFF_ROLES = {"ADMIN", "CATALOG_STAFF", "INVENTORY_STAFF", "ORDER_STAFF", "FINANCE_STAFF"};

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, CorsConfigurationSource cors) {
        ServerAuthenticationEntryPoint unauthorized = (exchange, ex) ->
                GatewayErrors.write(exchange, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                        "Authentication is required");
        ServerAccessDeniedHandler forbidden = (exchange, ex) ->
                GatewayErrors.write(exchange, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                        "You do not have permission to perform this action");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)       // API stateless, không dùng cookie
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(c -> c.configurationSource(cors))
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()     // CORS preflight
                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/logout", "/api/v1/auth/verify-email",
                                "/api/v1/auth/resend-verification", "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/categories/**", "/api/v1/brands/**",
                                "/api/v1/products/**", "/api/v1/media/**").permitAll()
                        // Webhook: xác thực bằng CHỮ KÝ HMAC ở payment-service, không bằng JWT
                        .pathMatchers(HttpMethod.POST, "/api/v1/webhooks/payments/**").permitAll()
                        // Trang thanh toán giả lập của mock provider (đóng vai "cổng thanh toán bên ngoài")
                        .pathMatchers("/api/v1/payments/mock/**").permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Tài liệu API (dev). Production có thể tắt bằng springdoc.swagger-ui.enabled=false
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/**", "/v3/api-docs/**",
                                "/docs/*/v3/api-docs/**").permitAll()
                        .pathMatchers("/actuator/**").denyAll()
                        // Kiểm tra thô ở gateway: /admin chỉ cho nhân viên. Service kiểm tra chi tiết từng quyền.
                        .pathMatchers("/api/v1/admin/**").hasAnyRole(STAFF_ROLES)
                        .anyExchange().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
                .build();
    }

    /** Public key lấy từ JWKS của identity-service, cache lại; gặp "kid" lạ thì tải lại. */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${app.security.jwk-set-uri}") String jwkSetUri,
                                         @Value("${app.security.issuer}") String issuer) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private static Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
