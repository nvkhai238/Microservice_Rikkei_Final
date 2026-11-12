package com.ecommerce.order.inventory;

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

/** Bản ghi giữ chỗ (FR-INV-007): mã, orderId, SKU, số lượng, trạng thái, hạn. */
@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Column(name = "sku_id", nullable = false)
    private UUID skuId;
    @Column(name = "warehouse_code", nullable = false)
    private String warehouseCode;
    @Column(nullable = false)
    private int quantity;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public Reservation(UUID orderId, UUID skuId, String warehouseCode, int quantity, Instant expiresAt, Instant now) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.skuId = skuId;
        this.warehouseCode = warehouseCode;
        this.quantity = quantity;
        this.status = ReservationStatus.ACTIVE;
        this.expiresAt = expiresAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void moveTo(ReservationStatus next, Instant now) {
        boolean allowed = switch (status) {
            case ACTIVE -> next == ReservationStatus.COMMITTED || next == ReservationStatus.RELEASED
                    || next == ReservationStatus.EXPIRED;
            case COMMITTED -> next == ReservationStatus.RESTOCKED;
            default -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Reservation " + id + " cannot move from " + status + " to " + next);
        }
        this.status = next;
        this.updatedAt = now;
    }
}
