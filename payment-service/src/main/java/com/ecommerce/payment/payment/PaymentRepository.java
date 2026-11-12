package com.ecommerce.payment.payment;

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

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    Optional<Payment> lockByOrderId(@Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.providerRef = :ref")
    Optional<Payment> lockByProviderRef(@Param("ref") String providerRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> lockById(@Param("id") UUID id);

    @Query("""
            SELECT p FROM Payment p
            WHERE (:status IS NULL OR p.status = :status) AND (:orderId IS NULL OR p.orderId = :orderId)
            """)
    Page<Payment> search(@Param("status") PaymentStatus status, @Param("orderId") UUID orderId, Pageable pageable);

    List<Payment> findByUpdatedAtAfterAndProviderRefIsNotNull(Instant since);
}
