package com.ecommerce.catalog.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsBySlug(String slug);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByBrandId(UUID brandId);

    Optional<Product> findBySlug(String slug);

    @Query("""
            SELECT p FROM Product p
            WHERE (:q IS NULL OR LOWER(p.name) LIKE CONCAT('%', :q, '%'))
              AND (:status IS NULL OR p.status = :status)
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
              AND (:brandId IS NULL OR p.brandId = :brandId)
            """)
    Page<Product> adminSearch(@Param("q") String q, @Param("status") ProductStatus status,
                              @Param("categoryId") UUID categoryId, @Param("brandId") UUID brandId, Pageable pageable);
}
