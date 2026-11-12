package com.ecommerce.gateway.error;

import com.ecommerce.gateway.filter.CorrelationIdWebFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Ghi body lỗi đúng format chung {code, message, correlationId} (COM-04).
 * Gateway là WebFlux, không dùng được common (servlet) nên có bản riêng gọn nhẹ.
 */
public final class GatewayErrors {
    private GatewayErrors() {
    }

    public static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = CorrelationIdWebFilter.get(exchange);
        String body = "{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"correlationId\":"
                + (correlationId == null ? "null" : "\"" + correlationId + "\"") + "}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
