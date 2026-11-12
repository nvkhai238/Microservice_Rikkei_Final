package com.ecommerce.order.order;

import java.util.EnumSet;
import java.util.Set;

/**
 * State machine trạng thái đơn theo ma trận SRS mục 7.1 (FR-ORD-010, BR-011).
 * <pre>
 *   PENDING ──> CONFIRMED ──> SHIPPING ──> COMPLETED
 *      │            │
 *      └──> CANCELLED <──┘        (SHIPPING -> CANCELLED: KHÔNG cho phép mặc định)
 * </pre>
 * COMPLETED và CANCELLED là trạng thái kết thúc.
 */
public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED;

    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(SHIPPING, CANCELLED);
            case SHIPPING -> EnumSet.of(COMPLETED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return allowedNext().isEmpty();
    }
}
