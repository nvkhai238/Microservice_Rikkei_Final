package com.ecommerce.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tiện ích băm và sinh token ngẫu nhiên.
 * Token gửi cho người dùng (refresh token, link xác minh...) chỉ lưu HASH trong DB:
 * lộ DB cũng không dùng được token (FR-IAM-005).
 */
public final class Hashing {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Hashing() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Token ngẫu nhiên an toàn, dạng base64url không padding (dùng được trong URL). */
    public static String randomToken(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    /** So sánh thời gian hằng định: chống đoán chữ ký theo thời gian phản hồi (timing attack). */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
