package com.ecommerce.order.inventory;

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

/** Một dòng sổ cái tồn kho, bất biến (FR-INV-004). Sửa sai bằng một dòng bù, không sửa dòng cũ. */
@Entity
@Immutable
@Table(name = "stock_movements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "sku_id", nullable = false)
    private UUID skuId;
    @Column(name = "warehouse_code", nullable = false)
    private String warehouseCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType type;
    @Column(nullable = false)
    private long quantity;
    @Column(name = "on_hand_delta", nullable = false)
    private long onHandDelta;
    @Column(name = "reserved_delta", nullable = false)
    private long reservedDelta;
    @Column(name = "on_hand_after", nullable = false)
    private long onHandAfter;
    @Column(name = "reserved_after", nullable = false)
    private long reservedAfter;
    @Column(name = "reference_type")
    private String referenceType;
    @Column(name = "reference_id")
    private String referenceId;
    private String reason;
    @Column(nullable = false)
    private String actor;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public StockMovement(StockLevel after, MovementType type, long quantity, long onHandDelta, long reservedDelta,
                         String referenceType, String referenceId, String reason, String actor, Instant now) {
        this.id = UUID.randomUUID();
        this.skuId = after.skuId();
        this.warehouseCode = after.warehouseCode();
        this.type = type;
        this.quantity = quantity;
        this.onHandDelta = onHandDelta;
        this.reservedDelta = reservedDelta;
        this.onHandAfter = after.onHand();
        this.reservedAfter = after.reserved();
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reason = reason;
        this.actor = actor;
        this.createdAt = now;
    }
}
