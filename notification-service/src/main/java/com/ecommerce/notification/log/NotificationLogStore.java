package com.ecommerce.notification.log;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Bảng notification_logs: trạng thái từng lần gửi (FR-NOT-005). */
@Repository
public class NotificationLogStore {
    private final JdbcTemplate jdbc;

    public NotificationLogStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Entry(UUID id, String status, int attempts) {
    }

    /**
     * Tạo dòng QUEUED nếu chưa có. Đã có (event trùng / retry) thì trả về dòng cũ để quyết định có gửi nữa không.
     */
    public Entry claim(UUID eventId, String eventType, String templateCode, int templateVersion, String channel,
                       String locale, UUID recipientUserId, String recipientMasked, String subject) {
        jdbc.update("""
                INSERT INTO notification_logs (id, event_id, event_type, template_code, template_version, channel, locale,
                    recipient_user_id, recipient_masked, subject, status, attempts, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'QUEUED', 0, now(), now())
                ON CONFLICT (event_id, channel, template_code) DO NOTHING
                """, UUID.randomUUID(), eventId, eventType, templateCode, templateVersion, channel, locale,
                recipientUserId, recipientMasked, subject);
        return find(eventId, channel, templateCode).orElseThrow();
    }

    public Optional<Entry> find(UUID eventId, String channel, String templateCode) {
        return jdbc.query("SELECT id, status, attempts FROM notification_logs WHERE event_id = ? AND channel = ? AND template_code = ?",
                (rs, i) -> new Entry(rs.getObject(1, UUID.class), rs.getString(2), rs.getInt(3)),
                eventId, channel, templateCode).stream().findFirst();
    }

    public void markSent(UUID id, String providerMessageId) {
        jdbc.update("""
                UPDATE notification_logs SET status = 'SENT', provider_message_id = ?, attempts = attempts + 1,
                    last_error = NULL, sent_at = now(), updated_at = now() WHERE id = ?
                """, providerMessageId, id);
    }

    public void markFailed(UUID id, String error) {
        jdbc.update("""
                UPDATE notification_logs SET status = 'FAILED', attempts = attempts + 1, last_error = ?, updated_at = now()
                WHERE id = ?
                """, error == null ? null : error.substring(0, Math.min(error.length(), 1000)), id);
    }

    public void recordSkipped(UUID eventId, String eventType, String templateCode, int version, String channel,
                              String locale, UUID userId, String reason) {
        jdbc.update("""
                INSERT INTO notification_logs (id, event_id, event_type, template_code, template_version, channel, locale,
                    recipient_user_id, status, attempts, last_error, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SKIPPED', 0, ?, now(), now())
                ON CONFLICT (event_id, channel, template_code) DO NOTHING
                """, UUID.randomUUID(), eventId, eventType, templateCode, version, channel, locale, userId, reason);
    }
}
