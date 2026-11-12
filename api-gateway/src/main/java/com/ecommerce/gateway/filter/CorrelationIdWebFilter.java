package com.ecommerce.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Chạy ĐẦU TIÊN trong gateway (trước cả Spring Security):
 * <ol>
 *   <li>Cấp/nhận correlation ID, gắn vào request đi xuống service và response trả về client (COM-05, FR-GW-007).</li>
 *   <li>XÓA các header danh tính do client tự gửi (X-User-Id, X-User-Roles): client không được giả mạo
 *       danh tính. Chỉ gateway được gắn các header này, sau khi verify JWT (FR-GW-004).</li>
 *   <li>Ghi access log có cấu trúc khi request kết thúc.</li>
 * </ol>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {
    public static final String HEADER = "X-Correlation-Id";
    private static final String ATTR = CorrelationIdWebFilter.class.getName() + ".id";
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("access-log");

    public static String get(ServerWebExchange exchange) {
        return exchange.getAttribute(ATTR);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String correlationId = incoming != null && VALID.matcher(incoming).matches()
                ? incoming : UUID.randomUUID().toString();
        exchange.getAttributes().put(ATTR, correlationId);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    h.set(HEADER, correlationId);
                    h.remove(IdentityHeaders.USER_ID);
                    h.remove(IdentityHeaders.USER_ROLES);
                })
                .build();
        exchange.getResponse().getHeaders().set(HEADER, correlationId);

        long start = System.nanoTime();
        ServerWebExchange mutated = exchange.mutate().request(request).build();
        return chain.filter(mutated).doFinally(signal -> {
            long ms = (System.nanoTime() - start) / 1_000_000;
            var status = mutated.getResponse().getStatusCode();
            String userId = mutated.getAttribute(IdentityHeaders.USER_ID_ATTR);
            String ip = request.getRemoteAddress() == null ? "-" : request.getRemoteAddress().getAddress().getHostAddress();
            // key=value: dễ parse bằng ELK/Loki. KHÔNG log header Authorization, query string chứa token... (NFR-SEC-009)
            ACCESS_LOG.info("method={} path={} status={} durationMs={} userId={} ip={} correlationId={}",
                    request.getMethod(), request.getPath().value(), status == null ? "-" : status.value(), ms,
                    userId == null ? "-" : userId, ip, correlationId);
        });
    }
}
