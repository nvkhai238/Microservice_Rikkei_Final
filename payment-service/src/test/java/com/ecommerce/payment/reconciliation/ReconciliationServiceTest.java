package com.ecommerce.payment.reconciliation;

import com.ecommerce.payment.payment.Payment;
import com.ecommerce.payment.provider.PaymentProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationServiceTest {

    private Payment pending() {
        return new Payment(UUID.randomUUID(), "ORD-1", UUID.randomUUID(), new BigDecimal("500"), "VND", "MOCK",
                Instant.now(), Instant.now());
    }

    @Test
    void detectsLostWebhook() {
        var ps = new PaymentProvider.ProviderStatus("PAID", new BigDecimal("500"), BigDecimal.ZERO);
        assertThat(ReconciliationService.compare(pending(), ps)).isEqualTo("PROVIDER_CAPTURED_BUT_INTERNAL_NOT");
    }

    @Test
    void matchingRecordsHaveNoIssue() {
        Payment p = pending();
        p.markSucceeded(new BigDecimal("500"), Instant.now());
        var ps = new PaymentProvider.ProviderStatus("PAID", new BigDecimal("500"), BigDecimal.ZERO);
        assertThat(ReconciliationService.compare(p, ps)).isNull();
    }

    @Test
    void detectsRefundDifference() {
        Payment p = pending();
        p.markSucceeded(new BigDecimal("500"), Instant.now());
        var ps = new PaymentProvider.ProviderStatus("PARTIALLY_REFUNDED", new BigDecimal("500"), new BigDecimal("100"));
        assertThat(ReconciliationService.compare(p, ps)).isEqualTo("REFUNDED_AMOUNT_DIFFERS");
    }
}
