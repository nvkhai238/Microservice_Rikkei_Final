package com.ecommerce.common.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Cấu hình chung cho SecurityFilterChain của mọi service. Mỗi service chỉ cần khai báo
 * đường dẫn nào public, đường dẫn nào cần role gì.
 * <ul>
 *   <li>Stateless (không session, không cookie) => tắt CSRF là an toàn (NFR-SEC-006: CSRF chỉ liên quan khi dùng cookie).</li>
 *   <li>Verify JWT bằng public key (resource server). Claim "roles" -> authority "ROLE_xxx".</li>
 *   <li>401/403 trả JSON đúng format chung.</li>
 * </ul>
 */
public final class SecurityDefaults {
    private SecurityDefaults() {
    }

    public static HttpSecurity apply(HttpSecurity http, JsonSecurityErrorHandler errorHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
    }

    public static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
