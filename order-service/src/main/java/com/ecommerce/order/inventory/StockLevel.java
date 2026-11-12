package com.ecommerce.order.inventory;

import java.util.UUID;

/** Kết quả đọc/ghi một dòng stocks. available = onHand - reserved (FR-INV-002). */
public record StockLevel(UUID skuId, String warehouseCode, long onHand, long reserved, long version) {
    public long available() {
        return onHand - reserved;
    }
}
