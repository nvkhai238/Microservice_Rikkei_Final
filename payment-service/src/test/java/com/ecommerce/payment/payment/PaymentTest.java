package com.ecommerce.payment.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {
    private final Instant now = Instant.now();

    private Payment captured(String amount) {
        Payment p = new Payment(UUID.randomUUID(), "ORD-1", UUID.randomUUID(), new BigDecimal(amount), "VND", "MOCK",
                now.plusSeconds(900), now);
        p.markSucceeded(new BigDecimal(amount), now);
        return p;
    }

    @Test
    void partialThenFullRefundUpdatesStatus() {
        Payment p = captured("100000");
        p.markRefundPending(now);
        p.applyRefund(new BigDecimal("40000"), now);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        p.applyRefund(new BigDecimal("60000"), now);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(p.isFullyRefunded()).isTrue();
    }

    @Test
    void failedRefundReturnsToCapturedState() {
        Payment p = captured("100000");
        p.markRefundPending(now);
        p.refundFailed(now);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        p.applyRefund(new BigDecimal("10000"), now);
        p.markRefundPending(now);
        p.refundFailed(now);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
    }
}
