package com.ecommerce.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:targetType IS NULL OR a.targetType = :targetType)
              AND (:targetId IS NULL OR a.targetId = :targetId)
              AND (:actorId IS NULL OR a.actorId = :actorId)
            """)
    Page<AuditLog> search(@Param("targetType") String targetType, @Param("targetId") String targetId,
                          @Param("actorId") String actorId, Pageable pageable);
}
