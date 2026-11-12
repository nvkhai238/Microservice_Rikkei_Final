package com.ecommerce.order.cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByCustomerId(UUID customerId);

    /** Tạo giỏ nếu chưa có. ON CONFLICT: hai request đầu tiên của cùng khách chạy song song vẫn chỉ tạo 1 giỏ. */
    @Modifying
    @Query(value = """
            INSERT INTO carts (id, customer_id, created_at, updated_at, version)
            VALUES (:id, :customerId, now(), now(), 0) ON CONFLICT (customer_id) DO NOTHING
            """, nativeQuery = true)
    int createIfAbsent(@Param("id") UUID id, @Param("customerId") UUID customerId);
}
