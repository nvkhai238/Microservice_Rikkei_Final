package com.ecommerce.payment.refund;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.payment.PaymentErrorCode;
import com.ecommerce.payment.payment.Payment;
import com.ecommerce.payment.payment.PaymentEvents;
import com.ecommerce.payment.payment.PaymentRepository;
import com.ecommerce.payment.payment.PaymentTransactionRepository;
import com.ecommerce.payment.provider.PaymentProviders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** BR-009 / FR-PAY-015: tổng hoàn không vượt số đã thu; FR-PAY-016: idempotent theo khóa. */
class RefundServiceTest {

    private final RefundRepository refunds = mock(RefundRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private RefundService service;
    private Payment payment;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new RefundService(refunds, payments, mock(PaymentTransactionRepository.class),
                mock(PaymentProviders.class), mock(PaymentEvents.class), mock(AuditService.class),
                mock(TransactionTemplate.class), clock);
        payment = new Payment(UUID.randomUUID(), "ORD-1", UUID.randomUUID(), new BigDecimal("100000"), "VND", "MOCK",
                clock.instant(), clock.instant());
        when(payments.lockById(payment.getId())).thenReturn(Optional.of(payment));
        when(refunds.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(refunds.sumPending(payment.getId())).thenReturn(BigDecimal.ZERO);
        when(refunds.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void cannotRefundUncapturedPayment() {
        assertThatThrownBy(() -> service.createPending(payment.getId(), BigDecimal.TEN, "x", "k1", "u"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_NOT_REFUNDABLE);
    }

    @Test
    void cannotRefundMoreThanCaptured() {
        payment.markSucceeded(new BigDecimal("100000"), Instant.now());
        assertThatThrownBy(() -> service.createPending(payment.getId(), new BigDecimal("100000.01"), "x", "k1", "u"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(PaymentErrorCode.REFUND_EXCEEDS_CAPTURED);
    }

    @Test
    void pendingRefundsCountTowardsTheLimit() {
        payment.markSucceeded(new BigDecimal("100000"), Instant.now());
        when(refunds.sumPending(payment.getId())).thenReturn(new BigDecimal("70000"));
        assertThatThrownBy(() -> service.createPending(payment.getId(), new BigDecimal("40000"), "x", "k1", "u"))
                .isInstanceOf(AppException.class);
        assertThat(service.createPending(payment.getId(), new BigDecimal("30000"), "x", "k2", "u")).isNotNull();
    }

    @Test
    void sameIdempotencyKeyReturnsExistingRefund() {
        payment.markSucceeded(new BigDecimal("100000"), Instant.now());
        Refund existing = new Refund(payment.getId(), payment.getOrderId(), new BigDecimal("1000"), "VND", "x",
                "same-key", "u", Instant.now());
        when(refunds.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existing));
        assertThat(service.createPending(payment.getId(), new BigDecimal("1000"), "x", "same-key", "u"))
                .isEqualTo(existing.getId());
        verify(refunds, never()).save(any());
    }
}
