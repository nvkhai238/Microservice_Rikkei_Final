package com.ecommerce.order.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Tính tiền bằng BigDecimal, scale 2, làm tròn HALF_UP (FR-CAT-008, DR-007).
 * Ví dụ vì sao không dùng double: 0.1 + 0.2 = 0.30000000000000004.
 */
public final class Money {
    public static final int SCALE = 2;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

    private Money() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity, BigDecimal discount) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity)).subtract(discount);
        if (total.signum() < 0) {
            throw new IllegalArgumentException("discount larger than line amount");
        }
        return normalize(total);
    }

    /** Tổng tiền đơn (FR-ORD-006): tạm tính - giảm giá + thuế + phí ship. v1: giảm giá/thuế/ship = 0 (TBD-05). */
    public record Totals(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal taxTotal, BigDecimal shippingFee,
                         BigDecimal grandTotal) {
        public static Totals of(List<OrderItem> items, BigDecimal taxTotal, BigDecimal shippingFee) {
            BigDecimal subtotal = normalize(items.stream()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            BigDecimal discount = normalize(items.stream().map(OrderItem::getDiscount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            BigDecimal grand = normalize(subtotal.subtract(discount).add(taxTotal).add(shippingFee));
            return new Totals(subtotal, discount, normalize(taxTotal), normalize(shippingFee), grand);
        }
    }
}
