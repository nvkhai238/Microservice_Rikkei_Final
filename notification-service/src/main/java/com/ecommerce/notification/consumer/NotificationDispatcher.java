package com.ecommerce.notification.consumer;

import com.ecommerce.notification.channel.NotificationChannel;
import com.ecommerce.notification.channel.PermanentDeliveryException;
import com.ecommerce.notification.log.NotificationLogStore;
import com.ecommerce.notification.recipient.Recipient;
import com.ecommerce.notification.template.TemplateRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gửi MỘT thông báo cho MỘT event, đúng một lần (FR-NOT-007):
 * <ol>
 *   <li>claim(): dòng log theo (eventId, kênh, template). Đã SENT => bỏ qua (event bị giao lại).</li>
 *   <li>Gửi qua kênh. Thành công => SENT + message id.</li>
 *   <li>Lỗi tạm thời => FAILED + ném lỗi => RabbitMQ retry có backoff; hết lượt => DLQ + cảnh báo (FR-NOT-006).</li>
 *   <li>Lỗi vĩnh viễn => FAILED, KHÔNG ném lỗi (retry vô ích).</li>
 * </ol>
 * Lỗi ở đây chỉ ảnh hưởng thông báo, không bao giờ làm hỏng đơn hàng/thanh toán (FR-NOT-008): chúng đã commit
 * ở service khác từ trước.
 */
@Slf4j
@Component
public class NotificationDispatcher {

    private final Map<String, NotificationChannel> channels;
    private final NotificationLogStore logs;
    private final TemplateRenderer templates;

    public NotificationDispatcher(List<NotificationChannel> channels, NotificationLogStore logs,
                                  TemplateRenderer templates) {
        this.channels = channels.stream().collect(Collectors.toMap(NotificationChannel::code, Function.identity()));
        this.logs = logs;
        this.templates = templates;
    }

    public void sendEmail(UUID eventId, String eventType, Recipient to, String templateCode, Map<String, String> vars) {
        NotificationChannel channel = channels.get("EMAIL");
        TemplateRenderer.Rendered rendered = templates.render("EMAIL", templateCode, vars);
        String masked = Recipient.mask(to.email());
        NotificationLogStore.Entry entry = logs.claim(eventId, eventType, templateCode, rendered.version(), "EMAIL",
                rendered.locale(), to.userId(), masked, rendered.subject());
        if ("SENT".equals(entry.status())) {
            log.info("Notification {} for event {} already sent, skipping", templateCode, eventId);
            return;
        }
        try {
            String messageId = channel.send(to.email(), rendered.subject(), rendered.body());
            logs.markSent(entry.id(), messageId);
            log.info("Sent {} email to {} (event {})", templateCode, masked, eventId);
        } catch (PermanentDeliveryException ex) {
            logs.markFailed(entry.id(), ex.getMessage());
            log.error("[ALERT] Permanent failure sending {} to {}: {}", templateCode, masked, ex.getMessage());
        } catch (RuntimeException ex) {
            logs.markFailed(entry.id(), ex.getMessage());
            log.warn("Sending {} to {} failed (attempt {}), will retry: {}", templateCode, masked,
                    entry.attempts() + 1, ex.getMessage());
            throw ex;
        }
    }

    public void skip(UUID eventId, String eventType, String templateCode, UUID userId, String reason) {
        logs.recordSkipped(eventId, eventType, templateCode, 1, "EMAIL", "vi", userId, reason);
        log.warn("Notification {} for event {} skipped: {}", templateCode, eventId, reason);
    }
}
