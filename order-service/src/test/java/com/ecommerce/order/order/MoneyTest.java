package com.ecommerce.order.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FR-CAT-008, FR-ORD-006: tính tiền bằng BigDecimal, kết quả xác định. */
class MoneyTest {

    @Test
    void lineTotalIsExact() {
        // Với double: 0.1 * 3 = 0.30000000000000004
        assertThat(Money.lineTotal(new BigDecimal("0.10"), 3, Money.ZERO)).isEqualByComparingTo("0.30");
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        assertThat(Money.normalize(new BigDecimal("10.005"))).isEqualByComparingTo("10.01");
        assertThat(Money.normalize(new BigDecimal("10.004"))).isEqualByComparingTo("10.00");
    }

    @Test
    void rejectsNonPositiveQuantityAndOverDiscount() {
        assertThatThrownBy(() -> Money.lineTotal(BigDecimal.TEN, 0, Money.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.lineTotal(BigDecimal.TEN, 1, new BigDecimal("11"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void totalsSumAllComponents() {
        List<OrderItem> items = List.of(
                new OrderItem(UUID.randomUUID(), "A", UUID.randomUUID(), "A", null, Map.of(), null, new BigDecimal("199000"), 2),
                new OrderItem(UUID.randomUUID(), "B", UUID.randomUUID(), "B", null, Map.of(), null, new BigDecimal("50000.50"), 1));
        Money.Totals t = Money.Totals.of(items, new BigDecimal("1000"), new BigDecimal("30000"));
        assertThat(t.subtotal()).isEqualByComparingTo("448000.50");
        assertThat(t.grandTotal()).isEqualByComparingTo("479000.50");
    }
}
