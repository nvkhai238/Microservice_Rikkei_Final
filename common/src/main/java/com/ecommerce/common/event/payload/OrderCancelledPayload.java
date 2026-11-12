package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * refundRequired = true nghĩa là tiền đã/đang được hoàn (đơn đã thanh toán).
 * Payment nghe event này để hủy payment intent còn PENDING (không cho thanh toán đơn đã hủy).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCancelledPayload(UUID orderId, String orderCode, UUID customerId, String reason,
                                    boolean refundRequired, BigDecimal amount, String currency, Instant cancelledAt) {
}
