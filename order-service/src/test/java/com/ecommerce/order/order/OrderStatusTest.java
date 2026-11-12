package com.ecommerce.order.order;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phủ TOÀN BỘ ma trận chuyển trạng thái SRS mục 7.1 (5 x 5 = 25 cặp): bước hợp lệ lẫn bị cấm.
 */
class OrderStatusTest {

    private static final Set<String> ALLOWED = Set.of(
            "PENDING->CONFIRMED", "PENDING->CANCELLED",
            "CONFIRMED->SHIPPING", "CONFIRMED->CANCELLED",
            "SHIPPING->COMPLETED");

    static Stream<Arguments> allPairs() {
        return Stream.of(OrderStatus.values())
                .flatMap(from -> Stream.of(OrderStatus.values()).map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allPairs")
    void transitionMatrixMatchesSrs(OrderStatus from, OrderStatus to) {
        boolean expected = ALLOWED.contains(from + "->" + to);
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("terminalStates")
    void terminalStatesHaveNoExit(OrderStatus status) {
        assertThat(status.isTerminal()).isTrue();
    }

    static Stream<OrderStatus> terminalStates() {
        return Stream.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED);
    }
}
