package com.ecommerce.identity.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String normalizedEmail);

    boolean existsByEmail(String normalizedEmail);

    @Query("""
            SELECT DISTINCT u FROM User u LEFT JOIN u.roles r
            WHERE (:q IS NULL OR u.email LIKE CONCAT('%', :q, '%') OR LOWER(u.fullName) LIKE CONCAT('%', :q, '%'))
              AND (:status IS NULL OR u.status = :status)
              AND (:role IS NULL OR r = :role)
            """)
    Page<User> search(@Param("q") String q, @Param("status") UserStatus status, @Param("role") String role,
                      Pageable pageable);
}
