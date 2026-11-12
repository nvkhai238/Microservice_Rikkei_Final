package com.ecommerce.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.CommonErrorCode;
import com.ecommerce.common.web.ServiceClients;
import com.ecommerce.order.config.OrderProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gọi catalog-service tra cứu SKU (giá hiện hành, còn bán không) - FR-CART-006, FR-ORD-003.
 * <ul>
 *   <li>Timeout ngắn (COM-06): catalog chậm thì báo lỗi nhanh, không treo thread.</li>
 *   <li>Retry tối đa 2 lần với exponential backoff + jitter, CHỈ cho lỗi tạm thời (mạng, 5xx).
 *       Đây là thao tác ĐỌC nên retry an toàn. Jitter tránh "bầy đàn" cùng retry một lúc.</li>
 *   <li>Circuit breaker (NFR-AVL-003): catalog lỗi liên tục (>= 50% trong 20 lần gọi gần nhất) thì "ngắt mạch"
 *       10 giây: các request sau báo 503 NGAY, không chờ timeout => không dồn thread, không lỗi dây chuyền.
 *       Hết 10 giây cho thử vài request; ổn thì đóng mạch lại.</li>
 * </ul>
 */
@Slf4j
@Component
public class CatalogClient {

    private final RestClient restClient;
    private final RetryTemplate retryTemplate;
    private final CircuitBreaker circuitBreaker = CircuitBreaker.of("catalog", CircuitBreakerConfig.custom()
            .slidingWindowSize(20)
            .minimumNumberOfCalls(10)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .permittedNumberOfCallsInHalfOpenState(3)
            .build());

    public CatalogClient(RestClient.Builder builder, OrderProperties props) {
        this.restClient = ServiceClients.create(builder, props.clients().catalogBaseUrl(),
                props.clients().connectTimeout(), props.clients().readTimeout());
        this.retryTemplate = new RetryTemplate(RetryPolicy.builder()
                .maxRetries(2)
                .delay(Duration.ofMillis(100))
                .multiplier(2)
                .jitter(Duration.ofMillis(50))
                .maxDelay(Duration.ofSeconds(1))
                .includes(ResourceAccessException.class, HttpServerErrorException.class)
                .build());
    }

    public Map<UUID, SkuSnapshot> lookup(Collection<UUID> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        if (!circuitBreaker.tryAcquirePermission()) {
            log.warn("Catalog circuit breaker is OPEN, failing fast");
            throw new AppException(CommonErrorCode.UPSTREAM_UNAVAILABLE);
        }
        long start = System.nanoTime();
        try {
            List<SkuSnapshot> result = retryTemplate.execute(() -> restClient.post()
                    .uri("/internal/v1/skus/lookup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("skuIds", List.copyOf(skuIds)))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<SkuSnapshot>>() {
                    }));
            circuitBreaker.onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            return result == null ? Map.of()
                    : result.stream().collect(Collectors.toMap(SkuSnapshot::skuId, Function.identity()));
        } catch (RuntimeException ex) {
            // Lỗi không retry (vd 4xx): không tính là catalog "chết"
            circuitBreaker.onSuccess(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            throw ex;
        } catch (RetryException ex) {
            circuitBreaker.onError(System.nanoTime() - start, TimeUnit.NANOSECONDS, ex);
            Throwable last = ex.getLastException();
            log.warn("Catalog lookup failed after retries: {}", last == null ? ex.getMessage() : last.getMessage());
            if (last != null && last.getCause() instanceof SocketTimeoutException) {
                throw new AppException(CommonErrorCode.UPSTREAM_TIMEOUT);
            }
            throw new AppException(CommonErrorCode.UPSTREAM_UNAVAILABLE);
        }
    }

    /** Bản sao dữ liệu catalog trả về (hợp đồng API nội bộ) */
    /** Bỏ qua field lạ: service kia thêm field mới không làm hỏng service này (NFR-COMP-001) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkuSnapshot(UUID skuId, String skuCode, UUID productId, String productName, String variantName,
                              Map<String, String> attributes, BigDecimal price, String currency, boolean sellable,
                              int maxPerOrder, String imageUrl) {
    }
}
