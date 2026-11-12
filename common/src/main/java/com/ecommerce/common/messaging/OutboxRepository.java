package com.ecommerce.common.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Lấy một lô event PENDING và KHÓA chúng lại.
     * SKIP LOCKED: nếu chạy 2 instance, instance thứ hai bỏ qua các dòng instance thứ nhất đang giữ
     * thay vì chờ => không gửi trùng và không nghẽn (FR-SAGA-008).
     */
    @Query(value = """
            SELECT * FROM outbox_event
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("limit") int limit);

    long countByStatus(OutboxEvent.Status status);

    /** Dọn event đã gửi lâu rồi để bảng không phình mãi. */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = com.ecommerce.common.messaging.OutboxEvent.Status.PUBLISHED AND e.publishedAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);
}
