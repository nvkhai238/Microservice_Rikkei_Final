package com.ecommerce.order.inventory;

import java.util.List;
import java.util.UUID;

/**
 * Không đủ hàng cho một hoặc nhiều dòng. Là RuntimeException => transaction ROLLBACK,
 * mọi dòng đã giữ trước đó trong cùng đơn tự hoàn lại (all-or-nothing, FR-INV-010).
 */
public class InsufficientStockException extends RuntimeException {
    private final List<Shortage> shortages;

    public InsufficientStockException(List<Shortage> shortages) {
        super("Insufficient stock for " + shortages.size() + " item(s)");
        this.shortages = List.copyOf(shortages);
    }

    public List<Shortage> getShortages() {
        return shortages;
    }

    public record Shortage(UUID skuId, long requested, long available) {
    }
}
