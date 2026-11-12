package com.ecommerce.common.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** amount là tổng tiền ĐÃ CHỐT trên đơn: payment lấy số này, không tin client (FR-PAY-002). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedPayload(
        UUID orderId,
        String orderCode,
        UUID customerId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        Instant reservationExpiresAt,
        List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String skuCode, String productName, String variantName, int quantity,
                       BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
