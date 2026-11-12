package com.ecommerce.order.order;

import com.ecommerce.common.exception.AppException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    private Order newOrder() {
        List<OrderItem> items = List.of(new OrderItem(UUID.randomUUID(), "SKU-1", UUID.randomUUID(), "Áo",
                "Đỏ/M", Map.of("size", "M"), null, new BigDecimal("199000"), 2));
        return Order.place("ORD-1", UUID.randomUUID(), "VND", "MOCK", null,
                new ShippingAddress("A", "+84900000000", "1 Lê Lợi", "P1", "Q1", "HCM", null), items,
                Money.Totals.of(items, BigDecimal.ZERO, BigDecimal.ZERO), now.plusSeconds(900), "customer", now);
    }

    @Test
    void placedOrderIsPendingAwaitingPaymentWithHistory() {
        Order order = newOrder();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.AWAITING_PAYMENT);
        assertThat(order.getGrandTotal()).isEqualByComparingTo("398000");
        assertThat(order.getHistory()).hasSize(1);
    }

    @Test
    void validTransitionIsRecordedWithActorAndReason() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMED, "Payment succeeded", "SYSTEM", now);
        var last = order.getHistory().get(order.getHistory().size() - 1);
        assertThat(last.getFromStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(last.getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(last.getActor()).isEqualTo("SYSTEM");
        assertThat(last.getReason()).isEqualTo("Payment succeeded");
    }

    @Test
    void invalidTransitionIsRejectedAndNothingChanges() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.COMPLETED, "skip", "staff", now))
                .isInstanceOf(AppException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getHistory()).hasSize(1);
    }

    @Test
    void cancellationKeepsReasonAndActor() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CANCELLED, "RESERVATION_EXPIRED", "SYSTEM", now);
        assertThat(order.getCancelReason()).isEqualTo("RESERVATION_EXPIRED");
        assertThat(order.getCancelledBy()).isEqualTo("SYSTEM");
    }
}
