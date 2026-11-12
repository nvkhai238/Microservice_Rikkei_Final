package com.ecommerce.catalog.internal;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * sellable = SKU ACTIVE + sản phẩm PUBLISHED + danh mục/thương hiệu ACTIVE.
 * order-service chụp snapshot các field này vào đơn hàng (FR-ORD-004).
 */
public record SkuSnapshot(UUID skuId, String skuCode, UUID productId, String productName, String variantName,
                          Map<String, String> attributes, BigDecimal price, String currency, boolean sellable,
                          int maxPerOrder, String imageUrl) {
}
