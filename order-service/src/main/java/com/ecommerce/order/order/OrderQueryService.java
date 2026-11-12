package com.ecommerce.order.order;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Đọc đơn. Khách chỉ thấy đơn của mình (FR-ORD-012, BR-013): đơn người khác => 404, không phải 403. */
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public PageResponse<OrderDtos.OrderSummary> myOrders(UUID customerId, Pageable pageable) {
        return PageResponse.of(orders.findByCustomerId(customerId, pageable), OrderDtos.OrderSummary::from);
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderView myOrder(UUID customerId, String idOrCode) {
        return OrderDtos.OrderView.from(find(idOrCode)
                .filter(o -> o.getCustomerId().equals(customerId))
                .orElseThrow(() -> AppException.notFound("Order")));
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderView anyOrder(String idOrCode) {
        return OrderDtos.OrderView.from(find(idOrCode).orElseThrow(() -> AppException.notFound("Order")));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDtos.OrderSummary> search(String code, UUID customerId, OrderStatus status, Instant from,
                                                       Instant to, Pageable pageable) {
        return PageResponse.of(orders.search(code == null || code.isBlank() ? null : code.trim().toUpperCase(),
                customerId, status, from, to, pageable), OrderDtos.OrderSummary::from);
    }

    public UUID resolveId(String idOrCode) {
        return find(idOrCode).map(Order::getId).orElseThrow(() -> AppException.notFound("Order"));
    }

    private Optional<Order> find(String idOrCode) {
        try {
            return orders.findById(UUID.fromString(idOrCode));
        } catch (IllegalArgumentException notUuid) {
            return orders.findByOrderCode(idOrCode.toUpperCase());
        }
    }
}
