package com.ecommerce.order.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    @Query("""
            SELECT m FROM StockMovement m
            WHERE (:skuId IS NULL OR m.skuId = :skuId)
              AND (:referenceId IS NULL OR m.referenceId = :referenceId)
            """)
    Page<StockMovement> search(@Param("skuId") UUID skuId, @Param("referenceId") String referenceId, Pageable pageable);
}
