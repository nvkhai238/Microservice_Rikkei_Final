package com.ecommerce.catalog.product;

import com.ecommerce.common.exception.AppException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributeNormalizationTest {

    @Test
    void keysAreLowercasedAndValuesTrimmed() {
        assertThat(ProductAdminService.normalizeAttributes(Map.of("Color", "  Đỏ ", "SIZE", "M")))
                .containsEntry("color", "Đỏ").containsEntry("size", "M");
    }

    @Test
    void rejectsInvalidKeysAndTooManyAttributes() {
        assertThatThrownBy(() -> ProductAdminService.normalizeAttributes(Map.of("bad key!", "x")))
                .isInstanceOf(AppException.class);
        Map<String, String> many = new HashMap<>();
        for (int i = 0; i < 11; i++) {
            many.put("k" + i, "v");
        }
        assertThatThrownBy(() -> ProductAdminService.normalizeAttributes(many)).isInstanceOf(AppException.class);
    }
}
