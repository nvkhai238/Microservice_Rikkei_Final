package com.ecommerce.notification.channel;

import com.ecommerce.notification.config.NotificationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Gửi email qua SMTP. Dev: Mailpit bắt hết mail, không gửi ra ngoài thật. */
@Component
@RequiredArgsConstructor
public class EmailChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final NotificationProperties props;

    @Override
    public String code() {
        return "EMAIL";
    }

    @Override
    public String send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(props.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            String messageId = "<" + UUID.randomUUID() + "@ecommerce.local>";
            message.saveChanges();
            message.setHeader("Message-ID", messageId);
            mailSender.send(message);
            return messageId;
        } catch (AddressException e) {
            throw new PermanentDeliveryException("Invalid recipient address", e);
        } catch (MessagingException e) {
            throw new IllegalStateException("Cannot build email", e);
        }
    }
}
