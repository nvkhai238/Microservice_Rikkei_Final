package com.ecommerce.payment.payment;

import com.ecommerce.common.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Column(name = "order_code", nullable = false)
    private String orderCode;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private String currency;
    @Column(nullable = false)
    private String provider;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    @Column(name = "provider_ref")
    private String providerRef;
    @Column(name = "checkout_url")
    private String checkoutUrl;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "captured_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal capturedAmount;
    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount;
    @Column(name = "failure_reason")
    private String failureReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public Payment(UUID orderId, String orderCode, UUID customerId, BigDecimal amount, String currency,
                   String provider, Instant expiresAt, Instant now) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
        this.status = PaymentStatus.PENDING;
        this.expiresAt = expiresAt;
        this.capturedAmount = BigDecimal.ZERO;
        this.refundedAmount = BigDecimal.ZERO;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void attachCheckout(String providerRef, String checkoutUrl, Instant now) {
        this.providerRef = providerRef;
        this.checkoutUrl = checkoutUrl;
        this.updatedAt = now;
    }

    public void markSucceeded(BigDecimal captured, Instant now) {
        this.status = PaymentStatus.SUCCEEDED;
        this.capturedAmount = captured;
        this.failureReason = null;
        this.updatedAt = now;
    }

    public void markFailed(String reason, Instant now) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = now;
    }

    public void close(PaymentStatus status, String reason, Instant now) {
        this.status = status;
        this.failureReason = reason;
        this.updatedAt = now;
    }

    public void markRefundPending(Instant now) {
        this.status = PaymentStatus.REFUND_PENDING;
        this.updatedAt = now;
    }

    public void applyRefund(BigDecimal amount, Instant now) {
        this.refundedAmount = this.refundedAmount.add(amount);
        this.status = refundedAmount.compareTo(capturedAmount) >= 0 ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
        this.updatedAt = now;
    }

    /** Hoàn tiền thất bại: quay về trạng thái "đã thu" tương ứng */
    public void refundFailed(Instant now) {
        this.status = refundedAmount.signum() > 0 ? PaymentStatus.PARTIALLY_REFUNDED : PaymentStatus.SUCCEEDED;
        this.updatedAt = now;
    }

    public boolean isFullyRefunded() {
        return status == PaymentStatus.REFUNDED;
    }
}
