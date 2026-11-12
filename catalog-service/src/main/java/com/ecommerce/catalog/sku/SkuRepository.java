package com.ecommerce.catalog.sku;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SkuRepository extends JpaRepository<Sku, UUID> {

    boolean existsBySkuCode(String skuCode);

    List<Sku> findByProductIdOrderByCreatedAtAsc(UUID productId);

    List<Sku> findByProductIdIn(List<UUID> productIds);

    /**
     * Cập nhật số lượng khả dụng từ event StockLevelChanged.
     * Điều kiện stock_version < :version: event CŨ đến muộn (sai thứ tự) sẽ không ghi đè số MỚI (FR-SAGA-007).
     */
    @Modifying
    @Query(value = """
            UPDATE skus SET available_quantity = :available, stock_version = :version
            WHERE id = :skuId AND stock_version < :version
            """, nativeQuery = true)
    int updateAvailability(@Param("skuId") UUID skuId, @Param("available") long available,
                           @Param("version") long version);
}
