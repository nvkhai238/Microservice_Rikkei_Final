package com.ecommerce.payment.payment;

import com.ecommerce.common.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Bút toán tài chính bất biến (FR-PAY-019). CAPTURE | FAILURE | REFUND. */
@Entity
@Immutable
@Table(name = "payment_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    @Column(nullable = false)
    private String type;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private String currency;
    @Column(name = "provider_ref")
    private String providerRef;
    @Column(name = "related_refund_id")
    private UUID relatedRefundId;
    private String note;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PaymentTransaction(UUID paymentId, String type, BigDecimal amount, String currency, String providerRef,
                              UUID relatedRefundId, String note, Instant now) {
        this.id = UUID.randomUUID();
        this.paymentId = paymentId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.providerRef = providerRef;
        this.relatedRefundId = relatedRefundId;
        this.note = note;
        this.createdAt = now;
    }
}
