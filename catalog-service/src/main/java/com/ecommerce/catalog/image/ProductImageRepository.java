package com.ecommerce.catalog.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderByPrimaryDescSortOrderAsc(UUID productId);

    long countByProductId(UUID productId);

    Optional<ProductImage> findByFileName(String fileName);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE ProductImage i SET i.primary = false WHERE i.productId = :productId AND i.primary = true")
    int clearPrimary(@Param("productId") UUID productId);
}
