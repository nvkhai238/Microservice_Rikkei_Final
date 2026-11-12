package com.ecommerce.catalog.brand;

import com.ecommerce.catalog.support.ActiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
    boolean existsBySlug(String slug);

    List<Brand> findAllByOrderByNameAsc();

    List<Brand> findByStatusOrderByNameAsc(ActiveStatus status);
}
