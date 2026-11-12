package com.ecommerce.notification.recipient;

import java.util.UUID;

public record Recipient(UUID userId, String email, String fullName) {

    /** "nguyenvana@gmail.com" -> "ng*******a@gmail.com" (FR-NOT-009, NFR-SEC-009) */
    public static String mask(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.length() <= 2 ? local.charAt(0) + "*"
                : local.substring(0, Math.min(2, local.length() - 1)) + "*".repeat(Math.max(1, local.length() - 3))
                + local.charAt(local.length() - 1);
        return masked + "@" + parts[1];
    }
}
