package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lệnh bù (compensation) của Saga hủy đơn: order yêu cầu payment hoàn tiền.
 * idempotencyKey cố định theo đơn => gửi lặp bao nhiêu lần cũng chỉ hoàn 1 lần (FR-SAGA-005).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RefundRequestedPayload(UUID orderId, String orderCode, UUID customerId, BigDecimal amount,
                                     String currency, String reason, String idempotencyKey) {
}
