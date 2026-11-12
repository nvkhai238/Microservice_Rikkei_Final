package com.ecommerce.identity.user;

import java.util.Locale;

/**
 * Chuẩn hóa định danh trước khi kiểm tra trùng / lưu (DR-011).
 * "  An@Gmail.COM " và "an@gmail.com" phải được coi là MỘT email.
 */
public final class Normalizer {
    private Normalizer() {
    }

    public static String email(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

    /** Số điện thoại VN về dạng +84xxxxxxxxx. Đầu vào đã qua @Pattern ở DTO. */
    public static String phone(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("[\\s.-]", "");
        if (digits.startsWith("+84")) {
            return digits;
        }
        if (digits.startsWith("84")) {
            return "+" + digits;
        }
        if (digits.startsWith("0")) {
            return "+84" + digits.substring(1);
        }
        return digits;
    }

    public static String text(String raw) {
        return raw == null ? null : raw.trim().replaceAll("\\s+", " ");
    }
}
