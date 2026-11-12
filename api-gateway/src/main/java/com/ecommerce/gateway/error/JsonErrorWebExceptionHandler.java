package com.ecommerce.gateway.error;

import io.netty.channel.ConnectTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

/**
 * FR-GW-002: upstream không khả dụng => trả lỗi CHUẨN (503/504) thay vì trang lỗi mặc định của Spring.
 * Order(-2) để chạy trước DefaultErrorWebExceptionHandler của Spring Boot (order -1).
 */
@Slf4j
@Component
@Order(-2)
public class JsonErrorWebExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        Throwable root = rootCause(ex);
        if (root instanceof ConnectException || root instanceof ConnectTimeoutException) {
            log.warn("Upstream unavailable for {}: {}", exchange.getRequest().getPath(), root.getMessage());
            return GatewayErrors.write(exchange, HttpStatus.SERVICE_UNAVAILABLE, "UPSTREAM_UNAVAILABLE",
                    "A dependent service is unavailable");
        }
        if (root instanceof TimeoutException) {
            return GatewayErrors.write(exchange, HttpStatus.GATEWAY_TIMEOUT, "UPSTREAM_TIMEOUT",
                    "A dependent service timed out");
        }
        if (ex instanceof ResponseStatusException rse) {
            HttpStatusCode status = rse.getStatusCode();
            return switch (status.value()) {
                case 404 -> GatewayErrors.write(exchange, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found");
                case 413 -> GatewayErrors.write(exchange, HttpStatus.CONTENT_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Payload too large");
                case 503 -> GatewayErrors.write(exchange, HttpStatus.SERVICE_UNAVAILABLE, "UPSTREAM_UNAVAILABLE",
                        "A dependent service is unavailable");
                case 504 -> GatewayErrors.write(exchange, HttpStatus.GATEWAY_TIMEOUT, "UPSTREAM_TIMEOUT",
                        "A dependent service timed out");
                default -> status.is4xxClientError()
                        ? GatewayErrors.write(exchange, HttpStatus.valueOf(status.value()), "VALIDATION_ERROR", "Invalid request")
                        : internal(exchange, ex);
            };
        }
        return internal(exchange, ex);
    }

    private Mono<Void> internal(ServerWebExchange exchange, Throwable ex) {
        log.error("Gateway error on {}", exchange.getRequest().getPath(), ex);
        return GatewayErrors.write(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred");
    }

    private static Throwable rootCause(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
