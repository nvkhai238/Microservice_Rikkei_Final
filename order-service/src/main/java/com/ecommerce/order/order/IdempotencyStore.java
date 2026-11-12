package com.ecommerce.order.order;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Lưu Idempotency-Key của request tạo đơn (FR-ORD-002).
 * <ul>
 *   <li>Cùng khóa + cùng nội dung => trả lại đơn đã tạo, KHÔNG tạo thêm.</li>
 *   <li>Cùng khóa + nội dung KHÁC => 409 IDEMPOTENCY_KEY_REUSED.</li>
 *   <li>Hai request cùng khóa chạy song song: INSERT thứ hai bị Postgres bắt CHỜ (unique index) tới khi
 *       cái đầu commit, rồi ON CONFLICT DO NOTHING trả 0 => biết đã có người làm rồi.</li>
 * </ul>
 */
@Repository
public class IdempotencyStore {

    public static final String CREATED = "CREATED";
    public static final String REJECTED_INSUFFICIENT_STOCK = "REJECTED_INSUFFICIENT_STOCK";

    private final JdbcTemplate jdbc;

    public IdempotencyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Entry(String requestHash, UUID orderId, String outcome) {
    }

    public Optional<Entry> find(UUID customerId, String key) {
        return jdbc.query("SELECT request_hash, order_id, outcome FROM idempotency_keys WHERE customer_id = ? AND idem_key = ?",
                (rs, i) -> new Entry(rs.getString(1), rs.getObject(2, UUID.class), rs.getString(3)),
                customerId, key).stream().findFirst();
    }

    /** @return true nếu giành được khóa (chưa ai dùng) */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(UUID customerId, String key, String requestHash, UUID orderId, String outcome) {
        return jdbc.update("""
                INSERT INTO idempotency_keys (customer_id, idem_key, request_hash, order_id, outcome, created_at)
                VALUES (?, ?, ?, ?, ?, now())
                ON CONFLICT (customer_id, idem_key) DO NOTHING
                """, customerId, key, requestHash, orderId, outcome) == 1;
    }
}
