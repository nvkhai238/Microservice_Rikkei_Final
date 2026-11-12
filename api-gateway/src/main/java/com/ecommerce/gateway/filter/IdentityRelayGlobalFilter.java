package com.ecommerce.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Sau khi JWT được verify: gắn X-User-Id / X-User-Roles vào request đi xuống service (FR-GW-004).
 * Service dùng các header này để LOG/TRACE; còn phân quyền thì service vẫn tự verify JWT
 * (header Authorization vẫn được chuyển tiếp nguyên vẹn).
 */
@Component
public class IdentityRelayGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> {
                    String userId = auth.getToken().getSubject();
                    List<String> roles = auth.getToken().getClaimAsStringList("roles");
                    exchange.getAttributes().put(IdentityHeaders.USER_ID_ATTR, userId);
                    return exchange.mutate().request(r -> r.headers(h -> {
                        h.set(IdentityHeaders.USER_ID, userId);
                        h.set(IdentityHeaders.USER_ROLES, roles == null ? "" : String.join(",", roles));
                    })).build();
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
