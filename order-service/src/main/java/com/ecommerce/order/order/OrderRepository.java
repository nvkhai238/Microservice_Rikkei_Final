package com.ecommerce.order.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Order> findByOrderCodeAndCustomerId(String code, UUID customerId);

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    /** Khóa đơn khi xử lý event Saga: event trùng/đồng thời trên cùng đơn chạy tuần tự. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> lockById(@Param("id") UUID id);

    /**
     * Đơn PENDING hết hạn giữ kho. SKIP LOCKED: chạy job trên nhiều instance cùng lúc thì mỗi instance
     * lấy một phần khác nhau, không xử lý trùng, không chờ nhau (FR-INV-015).
     */
    @Query(value = """
            SELECT id FROM orders
            WHERE status = 'PENDING' AND reservation_expires_at < :now
            ORDER BY reservation_expires_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> lockExpiredPending(@Param("now") Instant now, @Param("limit") int limit);

    /** FR-ORD-013: tìm đơn theo mã, khách hàng, trạng thái, khoảng thời gian. */
    @Query("""
            SELECT o FROM Order o
            WHERE (:code IS NULL OR o.orderCode = :code)
              AND (:customerId IS NULL OR o.customerId = :customerId)
              AND (:status IS NULL OR o.status = :status)
              AND (:fromTime IS NULL OR o.createdAt >= :fromTime)
              AND (:toTime IS NULL OR o.createdAt < :toTime)
            """)
    Page<Order> search(@Param("code") String code, @Param("customerId") UUID customerId,
                       @Param("status") OrderStatus status, @Param("fromTime") Instant from,
                       @Param("toTime") Instant to, Pageable pageable);
}
