package com.ecommerce.payment.mock;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * "Database của cổng thanh toán giả". Trong đời thực đây là hệ thống của VNPay/Stripe,
 * service mình KHÔNG đọc trực tiếp được; chỉ biết qua webhook và API của họ.
 */
@Repository
public class MockProviderStore {

    private final JdbcTemplate jdbc;

    public MockProviderStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Session(String providerRef, UUID orderRef, String orderCode, BigDecimal amount, String currency,
                          String status, BigDecimal refundedAmount) {
    }

    @Transactional
    public void create(String ref, UUID orderRef, String orderCode, BigDecimal amount, String currency) {
        jdbc.update("""
                INSERT INTO mock_provider_sessions (provider_ref, order_ref, order_code, amount, currency, status,
                    refunded_amount, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'OPEN', 0, now(), now())
                """, ref, orderRef, orderCode, amount, currency);
    }

    public Optional<Session> find(String ref) {
        return jdbc.query("SELECT * FROM mock_provider_sessions WHERE provider_ref = ?", (rs, i) -> new Session(
                rs.getString("provider_ref"), rs.getObject("order_ref", UUID.class), rs.getString("order_code"),
                rs.getBigDecimal("amount"), rs.getString("currency"), rs.getString("status"),
                rs.getBigDecimal("refunded_amount")), ref).stream().findFirst();
    }

    /** Chuyển trạng thái có điều kiện (chỉ từ OPEN) => bấm "Thanh toán" 2 lần cũng chỉ trả tiền 1 lần. */
    @Transactional
    public boolean transitionFromOpen(String ref, String newStatus) {
        return jdbc.update("UPDATE mock_provider_sessions SET status = ?, updated_at = now() WHERE provider_ref = ? AND status = 'OPEN'",
                newStatus, ref) == 1;
    }

    /** Hoàn tiền idempotent theo khóa: gọi lại cùng khóa => trả kết quả cũ. */
    @Transactional
    public Optional<String> refund(String ref, BigDecimal amount, String idempotencyKey) {
        var existing = jdbc.query("SELECT id FROM mock_provider_refunds WHERE idempotency_key = ?",
                (rs, i) -> rs.getString(1), idempotencyKey).stream().findFirst();
        if (existing.isPresent()) {
            return existing;
        }
        int updated = jdbc.update("""
                UPDATE mock_provider_sessions
                SET refunded_amount = refunded_amount + ?,
                    status = CASE WHEN refunded_amount + ? >= amount THEN 'REFUNDED' ELSE 'PARTIALLY_REFUNDED' END,
                    updated_at = now()
                WHERE provider_ref = ? AND status IN ('PAID', 'PARTIALLY_REFUNDED') AND refunded_amount + ? <= amount
                """, amount, amount, ref, amount);
        if (updated == 0) {
            return Optional.empty();
        }
        String refundId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        jdbc.update("INSERT INTO mock_provider_refunds (id, provider_ref, idempotency_key, amount, created_at) VALUES (?, ?, ?, ?, now())",
                refundId, ref, idempotencyKey, amount);
        return Optional.of(refundId);
    }
}
