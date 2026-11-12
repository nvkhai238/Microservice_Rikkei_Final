package com.ecommerce.common.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mã hóa AES-256-GCM cho dữ liệu nhạy cảm nằm trong event (vd: token đặt lại mật khẩu).
 * Chỉ identity (mã hóa) và notification (giải mã) giữ khóa EVENT_PAYLOAD_KEY;
 * ai đọc được RabbitMQ/DLQ/log cũng chỉ thấy chuỗi mã hóa.
 * <p>GCM vừa mã hóa vừa chống sửa (authenticated encryption). IV 12 byte ngẫu nhiên, ghép vào đầu ciphertext.
 */
@Component
public class PayloadCipher {
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String base64Key;

    public PayloadCipher(@Value("${app.crypto.event-payload-key:}") String base64Key) {
        this.base64Key = base64Key;
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot encrypt payload", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(Base64.getDecoder().decode(cipherText));
            byte[] iv = new byte[IV_LENGTH];
            buf.get(iv);
            byte[] encrypted = new byte[buf.remaining()];
            buf.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot decrypt payload", e);
        }
    }

    private SecretKeySpec key() {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("app.crypto.event-payload-key (EVENT_PAYLOAD_KEY) is not configured");
        }
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length != 32) {
            throw new IllegalStateException("EVENT_PAYLOAD_KEY must be 32 bytes encoded in base64");
        }
        return new SecretKeySpec(raw, "AES");
    }
}
