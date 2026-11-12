package com.ecommerce.payment.refund;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByIdempotencyKey(String key);

    List<Refund> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Refund r WHERE r.id = :id")
    Optional<Refund> lockById(@Param("id") UUID id);

    /** Tiền đang trong quá trình hoàn (chưa có kết quả) cũng phải tính vào hạn mức (BR-009). */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.paymentId = :paymentId AND r.status = 'PENDING'")
    BigDecimal sumPending(@Param("paymentId") UUID paymentId);

    @Query(value = """
            SELECT id FROM refunds WHERE status = 'PENDING' AND updated_at < :before
            ORDER BY updated_at LIMIT 20 FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> lockStuckPending(@Param("before") Instant before);
}
