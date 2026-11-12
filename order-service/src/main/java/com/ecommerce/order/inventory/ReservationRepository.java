package com.ecommerce.order.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByOrderId(UUID orderId);

    /** Khóa các reservation của đơn trước khi commit/release => hai luồng không cùng xử lý một reservation. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.orderId = :orderId AND r.status = :status ORDER BY r.skuId")
    List<Reservation> lockByOrderAndStatus(@Param("orderId") UUID orderId, @Param("status") ReservationStatus status);

    @Query("""
            SELECT r FROM Reservation r
            WHERE (:orderId IS NULL OR r.orderId = :orderId)
              AND (:skuId IS NULL OR r.skuId = :skuId)
              AND (:status IS NULL OR r.status = :status)
            """)
    Page<Reservation> search(@Param("orderId") UUID orderId, @Param("skuId") UUID skuId,
                             @Param("status") ReservationStatus status, Pageable pageable);
}
