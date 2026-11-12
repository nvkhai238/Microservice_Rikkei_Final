package com.ecommerce.order.order;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Mã đơn dạng ORD-261110-7KQ2M9XA (FR-ORD-007): ngày đặt + 8 ký tự ngẫu nhiên.
 * Không chứa PII (không SĐT/email), không đoán được đơn của người khác, dễ đọc qua điện thoại
 * (bảng chữ Crockford bỏ I, L, O, U dễ nhầm).
 */
@Component
public class OrderCodeGenerator {
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneOffset.UTC);
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public OrderCodeGenerator(Clock clock) {
        this.clock = clock;
    }

    public String next() {
        StringBuilder sb = new StringBuilder("ORD-").append(DATE.format(clock.instant())).append('-');
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
