package com.ecommerce.notification.recipient;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Read model người nhận (user_id -> email, tên), cập nhật từ event của identity. */
@Repository
public class RecipientStore {
    private final JdbcTemplate jdbc;

    public RecipientStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert(UUID userId, String email, String fullName) {
        jdbc.update("""
                INSERT INTO recipients (user_id, email, full_name, updated_at) VALUES (?, ?, ?, now())
                ON CONFLICT (user_id) DO UPDATE SET email = EXCLUDED.email, full_name = EXCLUDED.full_name,
                    updated_at = now()
                """, userId, email, fullName);
    }

    public Optional<Recipient> find(UUID userId) {
        return jdbc.query("SELECT user_id, email, full_name FROM recipients WHERE user_id = ?",
                (rs, i) -> new Recipient(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)), userId)
                .stream().findFirst();
    }
}
