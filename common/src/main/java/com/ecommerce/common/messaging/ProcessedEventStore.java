package com.ecommerce.common.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * "Bằng chứng đã xử lý" cho consumer idempotent (COM-08, FR-SAGA-009).
 * <p>Cách dùng trong handler (CÙNG transaction với thay đổi nghiệp vụ):
 * <pre>
 *   if (!processedEvents.markProcessed(eventId, "order.payment-events")) return; // đã xử lý rồi
 *   ... cập nhật đơn hàng ...
 * </pre>
 * INSERT ... ON CONFLICT DO NOTHING là nguyên tử: hai bản sao của cùng event chạy song song thì
 * bản sau phải chờ bản trước commit, rồi thấy đã tồn tại => trả false. Nếu bản trước rollback,
 * dòng đó cũng biến mất => bản sau được xử lý. Không bao giờ "mất" hoặc "xử lý 2 lần".
 */
@Component
public class ProcessedEventStore {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** @return true nếu đây là lần đầu event này được consumer xử lý */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(UUID eventId, String consumer) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO processed_event (event_id, consumer, processed_at)
                VALUES (?, ?, now())
                ON CONFLICT (event_id, consumer) DO NOTHING
                """, eventId, consumer);
        return inserted == 1;
    }
}
