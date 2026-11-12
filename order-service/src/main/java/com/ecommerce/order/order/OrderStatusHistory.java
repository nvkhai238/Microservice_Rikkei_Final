package com.ecommerce.order.order;

import com.ecommerce.common.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/** FR-ORD-011: trạng thái cũ, mới, lý do, tác nhân, thời điểm. Chỉ thêm (trigger DB chặn sửa/xóa). */
@Entity
@Immutable
@Table(name = "order_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory extends AssignedIdEntity {
    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private OrderStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private OrderStatus toStatus;
    private String reason;
    @Column(nullable = false)
    private String actor;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    OrderStatusHistory(OrderStatus from, OrderStatus to, String reason, String actor, Instant now) {
        this.id = UUID.randomUUID();
        this.fromStatus = from;
        this.toStatus = to;
        this.reason = reason;
        this.actor = actor;
        this.createdAt = now;
    }
}
