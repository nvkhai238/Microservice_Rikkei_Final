package com.ecommerce.order.saga;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaInstanceRepository extends JpaRepository<SagaInstance, UUID> {

    Optional<SagaInstance> findByOrderIdAndType(UUID orderId, SagaType type);

    List<SagaInstance> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    @Query("""
            SELECT s FROM SagaInstance s
            WHERE (:status IS NULL OR s.status = :status) AND (:type IS NULL OR s.type = :type)
            """)
    Page<SagaInstance> search(@Param("status") SagaStatus status, @Param("type") SagaType type, Pageable pageable);
}
