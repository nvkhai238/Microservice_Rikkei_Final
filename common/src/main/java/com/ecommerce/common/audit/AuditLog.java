package com.ecommerce.common.audit;

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

/**
 * Nhật ký audit: ai, làm gì, trên đối tượng nào, trước/sau, lúc nào, từ đâu, kết quả.
 * Chỉ INSERT: @Immutable phía Hibernate + trigger chặn UPDATE/DELETE phía DB (FR-ADM-003).
 */
@Entity
@Immutable
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "actor_id", nullable = false)
    private String actorId;
    @Column(nullable = false)
    private String action;
    @Column(name = "target_type", nullable = false)
    private String targetType;
    @Column(name = "target_id")
    private String targetId;
    @Column(name = "before_data", columnDefinition = "text")
    private String beforeData;
    @Column(name = "after_data", columnDefinition = "text")
    private String afterData;
    @Column(nullable = false)
    private String result;
    @Column(name = "correlation_id")
    private String correlationId;
    @Column(name = "client_ip")
    private String clientIp;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuditLog(String actorId, String action, String targetType, String targetId, String beforeData,
                    String afterData, String result, String correlationId, String clientIp, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.result = result;
        this.correlationId = correlationId;
        this.clientIp = clientIp;
        this.createdAt = createdAt;
    }
}
