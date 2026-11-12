package com.ecommerce.payment.refund;

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

/** Yêu cầu hoàn tiền: có lý do, người yêu cầu, khóa idempotency (FR-PAY-016). */
@Entity
@Table(name = "refunds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private String currency;
    @Column(nullable = false)
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;
    @Column(name = "requested_by", nullable = false)
    private String requestedBy;
    @Column(name = "provider_ref")
    private String providerRef;
    @Column(name = "failure_reason")
    private String failureReason;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public Refund(UUID paymentId, UUID orderId, BigDecimal amount, String currency, String reason,
                  String idempotencyKey, String requestedBy, Instant now) {
        this.id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
        this.idempotencyKey = idempotencyKey;
        this.requestedBy = requestedBy;
        this.status = RefundStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void retry(Instant now) {
        this.status = RefundStatus.PENDING;
        this.failureReason = null;
        this.updatedAt = now;
    }

    void attempted(Instant now) {
        this.attempts++;
        this.updatedAt = now;
    }

    void succeeded(String providerRef, Instant now) {
        this.status = RefundStatus.SUCCEEDED;
        this.providerRef = providerRef;
        this.updatedAt = now;
    }

    void failed(String reason, Instant now) {
        this.status = RefundStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = now;
    }
}
