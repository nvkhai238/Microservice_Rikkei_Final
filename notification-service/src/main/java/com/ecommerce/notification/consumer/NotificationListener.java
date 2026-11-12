package com.ecommerce.notification.consumer;

import com.ecommerce.common.crypto.PayloadCipher;
import com.ecommerce.common.event.EventEnvelope;
import com.ecommerce.common.event.EventTypes;
import com.ecommerce.common.event.payload.AccountTokenPayload;
import com.ecommerce.common.event.payload.OrderCancelledPayload;
import com.ecommerce.common.event.payload.OrderConfirmedPayload;
import com.ecommerce.common.event.payload.OrderCreatedPayload;
import com.ecommerce.common.event.payload.OrderStatusChangedPayload;
import com.ecommerce.common.event.payload.PaymentFailedPayload;
import com.ecommerce.common.event.payload.RefundCompletedPayload;
import com.ecommerce.common.event.payload.UserRegisteredPayload;
import com.ecommerce.common.messaging.EventMessages;
import com.ecommerce.common.messaging.EventTopology;
import com.ecommerce.common.web.CorrelationId;
import com.ecommerce.notification.config.NotificationProperties;
import com.ecommerce.notification.recipient.Recipient;
import com.ecommerce.notification.recipient.RecipientStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Chuyển event nghiệp vụ thành thông báo cho khách (FR-NOT-001). Nội dung lấy TỪ EVENT, không gọi DB service khác
 * (FR-NOT-004). Giờ hiển thị theo Asia/Ho_Chi_Minh (UI-10), dữ liệu gốc vẫn là UTC.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(VN);

    private final EventMessages eventMessages;
    private final RecipientStore recipients;
    private final NotificationDispatcher dispatcher;
    private final PayloadCipher cipher;
    private final NotificationProperties props;

    @RabbitListener(queues = EventTopology.NOTIFICATION_QUEUE)
    public void onMessage(Message message) {
        EventEnvelope<JsonNode> env = eventMessages.parse(message);
        CorrelationId.runWith(env.correlationId(), () -> handle(env));
    }

    private void handle(EventEnvelope<JsonNode> env) {
        UUID eventId = env.eventId();
        switch (env.eventType()) {
            case EventTypes.USER_REGISTERED -> {
                var p = eventMessages.payload(env, UserRegisteredPayload.class);
                recipients.upsert(p.userId(), p.email(), p.fullName());
                dispatcher.sendEmail(eventId, env.eventType(), new Recipient(p.userId(), p.email(), p.fullName()),
                        "user-welcome", Map.of("name", p.fullName()));
            }
            case EventTypes.EMAIL_VERIFICATION_REQUESTED, EventTypes.PASSWORD_RESET_REQUESTED -> {
                var p = eventMessages.payload(env, AccountTokenPayload.class);
                recipients.upsert(p.userId(), p.email(), p.fullName());
                boolean verify = EventTypes.EMAIL_VERIFICATION_REQUESTED.equals(env.eventType());
                // Token chỉ được GIẢI MÃ trong bộ nhớ để dựng link, không bao giờ lưu/log
                String token = URLEncoder.encode(cipher.decrypt(p.encryptedToken()), StandardCharsets.UTF_8);
                String link = props.frontendBaseUrl() + (verify ? "/verify-email?token=" : "/reset-password?token=") + token;
                dispatcher.sendEmail(eventId, env.eventType(), new Recipient(p.userId(), p.email(), p.fullName()),
                        verify ? "email-verification" : "password-reset",
                        Map.of("name", p.fullName(), "link", link, "expiresAt", TIME.format(p.expiresAt())));
            }
            case EventTypes.ORDER_CREATED -> {
                var p = eventMessages.payload(env, OrderCreatedPayload.class);
                StringBuilder rows = new StringBuilder();
                p.items().forEach(i -> rows.append("<tr><td>").append(HtmlUtils.htmlEscape(i.productName()))
                        .append(i.variantName() == null ? "" : " (" + HtmlUtils.htmlEscape(i.variantName()) + ")")
                        .append("</td><td style=\"text-align:center\">").append(i.quantity())
                        .append("</td><td style=\"text-align:right\">").append(money(i.lineTotal(), p.currency()))
                        .append("</td></tr>"));
                toCustomer(eventId, env.eventType(), p.customerId(), "order-created", Map.of(
                        "orderCode", p.orderCode(), "total", money(p.amount(), p.currency()),
                        "expiresAt", TIME.format(p.reservationExpiresAt()), "itemsHtml", rows.toString(),
                        "orderLink", props.frontendBaseUrl() + "/orders/" + p.orderCode()));
            }
            case EventTypes.ORDER_CONFIRMED -> {
                var p = eventMessages.payload(env, OrderConfirmedPayload.class);
                toCustomer(eventId, env.eventType(), p.customerId(), "order-confirmed", Map.of(
                        "orderCode", p.orderCode(), "total", money(p.amount(), p.currency()),
                        "confirmedAt", TIME.format(p.confirmedAt())));
            }
            case EventTypes.ORDER_STATUS_CHANGED -> {
                var p = eventMessages.payload(env, OrderStatusChangedPayload.class);
                // CONFIRMED đã có mail riêng, CANCELLED có OrderCancelled => chỉ báo giao hàng/hoàn tất ở đây
                if ("SHIPPING".equals(p.newStatus()) || "COMPLETED".equals(p.newStatus())) {
                    toCustomer(eventId, env.eventType(), p.customerId(), "order-status-changed", Map.of(
                            "orderCode", p.orderCode(), "status", statusVi(p.newStatus()),
                            "at", TIME.format(p.occurredAt())));
                }
            }
            case EventTypes.ORDER_CANCELLED -> {
                var p = eventMessages.payload(env, OrderCancelledPayload.class);
                toCustomer(eventId, env.eventType(), p.customerId(), "order-cancelled", Map.of(
                        "orderCode", p.orderCode(), "reason", reasonVi(p.reason()),
                        "refundNote", p.refundRequired()
                                ? "Số tiền " + money(p.amount(), p.currency()) + " đã được hoàn về phương thức thanh toán của bạn."
                                : ""));
            }
            case EventTypes.PAYMENT_FAILED -> {
                var p = eventMessages.payload(env, PaymentFailedPayload.class);
                toCustomer(eventId, env.eventType(), p.customerId(), "payment-failed", Map.of(
                        "reason", p.failureCategory()));
            }
            case EventTypes.REFUND_COMPLETED -> {
                var p = eventMessages.payload(env, RefundCompletedPayload.class);
                toCustomer(eventId, env.eventType(), p.customerId(), "refund-completed", Map.of(
                        "amount", money(p.amount(), p.currency()),
                        "total", money(p.totalRefunded(), p.currency())));
            }
            default -> log.debug("No notification for {}", env.eventType());
        }
    }

    private void toCustomer(UUID eventId, String eventType, UUID customerId, String template, Map<String, String> vars) {
        var recipient = recipients.find(customerId).orElse(null);
        if (recipient == null) {
            dispatcher.skip(eventId, eventType, template, customerId, "RECIPIENT_UNKNOWN");
            return;
        }
        Map<String, String> all = new HashMap<>(vars);
        all.put("name", recipient.fullName());
        dispatcher.sendEmail(eventId, eventType, recipient, template, all);
    }

    private static String money(BigDecimal amount, String currency) {
        NumberFormat f = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        f.setMaximumFractionDigits("VND".equals(currency) ? 0 : 2);
        return f.format(amount) + " " + ("VND".equals(currency) ? "₫" : currency);
    }

    private static String statusVi(String status) {
        return switch (status) {
            case "SHIPPING" -> "Đang giao hàng";
            case "COMPLETED" -> "Đã giao thành công";
            default -> status;
        };
    }

    private static String reasonVi(String reason) {
        if (reason == null) {
            return "";
        }
        return switch (reason) {
            case "RESERVATION_EXPIRED" -> "Đơn hàng quá thời hạn thanh toán";
            case "PAYMENT_DECLINED" -> "Thanh toán không thành công";
            default -> reason;
        };
    }

}
