package com.ecommerce.order.saga;

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

import java.time.Instant;
import java.util.UUID;

/** Một lần chạy Saga: định danh, trạng thái, correlation ID, bước hiện tại (FR-SAGA-001). */
@Entity
@Table(name = "saga_instances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaInstance extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Enumerated(EnumType.STRING)
    @Column(name = "saga_type", nullable = false)
    private SagaType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;
    @Column(name = "current_step", nullable = false)
    private String currentStep;
    @Column(name = "correlation_id")
    private String correlationId;
    @Column(name = "last_error")
    private String lastError;
    @Column(name = "retry_count", nullable = false)
    private int retryCount;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public SagaInstance(UUID orderId, SagaType type, SagaStatus status, String step, String correlationId,
                        Instant now) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.type = type;
        this.status = status;
        this.currentStep = step;
        this.correlationId = correlationId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void advance(SagaStatus status, String step, Instant now) {
        this.status = status;
        this.currentStep = step;
        this.updatedAt = now;
        if (status != SagaStatus.FAILED) {
            this.lastError = null;
        }
    }

    void fail(String step, String error, Instant now) {
        this.status = SagaStatus.FAILED;
        this.currentStep = step;
        this.lastError = error == null || error.length() <= 1000 ? error : error.substring(0, 1000);
        this.updatedAt = now;
    }

    void markRetried(Instant now) {
        this.retryCount++;
        this.updatedAt = now;
    }
}
