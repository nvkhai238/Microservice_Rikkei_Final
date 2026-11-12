package com.ecommerce.order.saga;

import com.ecommerce.common.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/** Một bước trong lịch sử Saga (FR-SAGA-001). Chỉ thêm, không sửa. */
@Entity
@Immutable
@Table(name = "saga_steps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaStep extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;
    @Column(nullable = false)
    private String step;
    /** DONE | FAILED | COMPENSATED | INFO */
    @Column(nullable = false)
    private String status;
    private String detail;
    @Column(nullable = false)
    private String actor;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SagaStep(UUID sagaId, String step, String status, String detail, String actor, Instant now) {
        this.id = UUID.randomUUID();
        this.sagaId = sagaId;
        this.step = step;
        this.status = status;
        this.detail = detail == null || detail.length() <= 1000 ? detail : detail.substring(0, 1000);
        this.actor = actor;
        this.createdAt = now;
    }
}
