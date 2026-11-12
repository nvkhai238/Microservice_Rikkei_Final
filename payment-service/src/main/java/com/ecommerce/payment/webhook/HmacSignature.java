package com.ecommerce.payment.webhook;

import com.ecommerce.common.crypto.Hashing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Chữ ký webhook kiểu Stripe: header {@code X-Mock-Signature: t=<epoch giây>,v1=<hex HMAC-SHA256>}
 * với nội dung ký = "t" + "." + body gốc.
 * <ul>
 *   <li>HMAC với secret chung: chỉ ai có secret mới tạo được chữ ký đúng => chống giả mạo (FR-PAY-007).</li>
 *   <li>Timestamp nằm TRONG phần được ký: kẻ gian bắt được webhook cũ cũng không sửa t được;
 *       t quá cũ => từ chối => chống replay (FR-PAY-008).</li>
 *   <li>So sánh constant-time: không lộ chữ ký qua thời gian phản hồi.</li>
 * </ul>
 */
public final class HmacSignature {
    private HmacSignature() {
    }

    public static String header(String secret, Instant timestamp, String body) {
        long t = timestamp.getEpochSecond();
        return "t=" + t + ",v1=" + hmacHex(secret, t + "." + body);
    }

    public enum Result { VALID, INVALID, EXPIRED }

    public static Result verify(String secret, String header, String body, Instant now, Duration window) {
        if (header == null || secret == null || secret.isBlank()) {
            return Result.INVALID;
        }
        Long t = null;
        String v1 = null;
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            if (kv[0].equals("t")) {
                try {
                    t = Long.parseLong(kv[1]);
                } catch (NumberFormatException e) {
                    return Result.INVALID;
                }
            } else if (kv[0].equals("v1")) {
                v1 = kv[1];
            }
        }
        if (t == null || v1 == null) {
            return Result.INVALID;
        }
        if (!Hashing.constantTimeEquals(hmacHex(secret, t + "." + body), v1)) {
            return Result.INVALID;
        }
        long age = Math.abs(now.getEpochSecond() - t);
        return age > window.toSeconds() ? Result.EXPIRED : Result.VALID;
    }

    static String hmacHex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
