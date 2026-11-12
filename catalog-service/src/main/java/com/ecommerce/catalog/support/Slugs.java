package com.ecommerce.catalog.support;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Sinh slug thân thiện URL từ tên tiếng Việt: "Áo Thun Nam Đỏ" -> "ao-thun-nam-do" (DR-011).
 */
public final class Slugs {
    private Slugs() {
    }

    public static String slugify(String input) {
        String noAccent = Normalizer.normalize(input.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = noAccent.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.length() > 100) {
            slug = slug.substring(0, 100).replaceAll("-$", "");
        }
        return slug.isEmpty() ? "item" : slug;
    }

    /** Thêm hậu tố -2, -3... cho tới khi không trùng. */
    public static String unique(String base, Predicate<String> exists) {
        String candidate = base;
        int i = 2;
        while (exists.test(candidate)) {
            candidate = base + "-" + i++;
        }
        return candidate;
    }

    public static boolean isValid(String slug) {
        return slug != null && slug.matches("^[a-z0-9]+(-[a-z0-9]+)*$") && slug.length() <= 120;
    }
}
