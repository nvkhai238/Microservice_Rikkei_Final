package com.ecommerce.order.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CartDtos {
    private CartDtos() {
    }

    public record AddItemRequest(@NotNull UUID skuId, @NotNull @Min(1) @Max(999) Integer quantity) {
    }

    public record UpdateItemRequest(@NotNull @Min(1) @Max(999) Integer quantity) {
    }

    /**
     * Dòng giỏ hàng (UI-03). warnings: PRICE_CHANGED, NOT_SELLABLE, INSUFFICIENT_STOCK, QUANTITY_LIMIT_EXCEEDED,
     * CATALOG_UNAVAILABLE (FR-CART-006, FR-CART-007).
     */
    public record CartLine(UUID skuId, String skuCode, String productName, String variantName,
                           Map<String, String> attributes, String imageUrl, int quantity, BigDecimal unitPrice,
                           BigDecimal previousUnitPrice, BigDecimal lineTotal, boolean sellable, long availableQuantity,
                           int maxPerOrder, List<String> warnings) {
    }

    /** subtotal chỉ là ƯỚC TÍNH; tổng chính thức chốt lúc tạo đơn (FR-CART-008). */
    public record CartView(UUID cartId, List<CartLine> items, BigDecimal estimatedSubtotal, String currency,
                           boolean checkoutReady, List<String> warnings) {
    }
}
