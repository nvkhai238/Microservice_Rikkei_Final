package com.ecommerce.gateway.ratelimit;

import com.ecommerce.gateway.error.GatewayErrors;
import com.ecommerce.gateway.filter.IdentityHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Rate limit kiểu "fixed window" trên Redis (FR-GW-005, NFR-SEC-007):
 * key = rl:{policy}:{user hoặc IP}:{số thứ tự cửa sổ}. Mỗi request INCR; vượt limit => 429.
 * <ul>
 *   <li>Đếm trong Redis => nhiều instance gateway dùng chung bộ đếm.</li>
 *   <li>Redis sập => cho qua (fail-open) và log cảnh báo: thà mất rate limit vài phút còn hơn sập cả hệ thống.</li>
 * </ul>
 * Chạy SAU Spring Security nên đã biết userId (attribute do IdentityRelayGlobalFilter gắn).
 */
@Slf4j
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {
    private static final long FAIL_OPEN = -1L;

    private final ReactiveStringRedisTemplate redis;
    private final RateLimitProperties props;
    private final List<CompiledPolicy> policies;

    public RateLimitGlobalFilter(ReactiveStringRedisTemplate redis, RateLimitProperties props) {
        this.redis = redis;
        this.props = props;
        PathPatternParser parser = new PathPatternParser();
        this.policies = props.policies() == null ? List.of() : props.policies().stream()
                .map(p -> new CompiledPolicy(p, p.paths().stream().map(parser::parse).toList()))
                .toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!props.enabled()) {
            return chain.filter(exchange);
        }
        PathContainer path = exchange.getRequest().getPath().pathWithinApplication();
        String method = exchange.getRequest().getMethod().name();
        CompiledPolicy match = policies.stream().filter(p -> p.matches(path, method)).findFirst().orElse(null);
        if (match == null) {
            return chain.filter(exchange);
        }
        RateLimitProperties.Policy policy = match.policy();
        String subject = subject(exchange, policy.key());
        long windowMillis = policy.window().toMillis();
        long windowIndex = System.currentTimeMillis() / windowMillis;
        String key = "rl:" + policy.name() + ":" + subject + ":" + windowIndex;

        // Chỉ bắt lỗi của REDIS ở đây. Lỗi của service phía sau (chain.filter) phải để lọt ra ngoài
        // cho JsonErrorWebExceptionHandler, không được nuốt nhầm.
        Mono<Long> counter = redis.opsForValue().increment(key)
                .flatMap(count -> (count == 1 ? redis.expire(key, policy.window().plusSeconds(1)) : Mono.just(true))
                        .thenReturn(count))
                .onErrorResume(ex -> {
                    log.warn("Rate limiter unavailable, allowing request: {}", ex.getMessage());
                    return Mono.just(FAIL_OPEN);
                });

        return counter.flatMap(count -> {
            if (count == FAIL_OPEN) {
                return chain.filter(exchange);
            }
            long remaining = Math.max(0, policy.limit() - count);
            exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(policy.limit()));
            exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(remaining));
            if (count > policy.limit()) {
                long retryAfter = Duration.ofMillis(windowMillis - System.currentTimeMillis() % windowMillis)
                        .toSeconds() + 1;
                exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(retryAfter));
                log.info("Rate limit exceeded policy={} subject={}", policy.name(), subject);
                return GatewayErrors.write(exchange, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                        "Too many requests, please retry later");
            }
            return chain.filter(exchange);
        });
    }

    private static String subject(ServerWebExchange exchange, RateLimitProperties.KeyType type) {
        String userId = exchange.getAttribute(IdentityHeaders.USER_ID_ATTR);
        String ip = exchange.getRequest().getRemoteAddress() == null ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        return switch (type) {
            case IP -> "ip:" + ip;
            case USER -> userId != null ? "user:" + userId : "ip:" + ip;
            case USER_OR_IP -> userId != null ? "user:" + userId : "ip:" + ip;
        };
    }

    @Override
    public int getOrder() {
        // Sau IdentityRelayGlobalFilter (cần userId), trước khi route tới service
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private record CompiledPolicy(RateLimitProperties.Policy policy, List<PathPattern> patterns) {
        boolean matches(PathContainer path, String method) {
            boolean methodOk = policy.methods() == null || policy.methods().isEmpty()
                    || policy.methods().stream().anyMatch(m -> m.equalsIgnoreCase(method));
            return methodOk && patterns.stream().anyMatch(p -> p.matches(path));
        }
    }

}
