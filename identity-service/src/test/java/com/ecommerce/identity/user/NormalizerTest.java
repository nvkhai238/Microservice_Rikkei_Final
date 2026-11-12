package com.ecommerce.identity.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** DR-011: chuẩn hóa định danh trước khi kiểm tra trùng. */
class NormalizerTest {

    @Test
    void emailIsTrimmedAndLowercased() {
        assertThat(Normalizer.email("  An.Nguyen@Gmail.COM ")).isEqualTo("an.nguyen@gmail.com");
    }

    @Test
    void vietnamesePhonesNormalizeToE164() {
        assertThat(Normalizer.phone("0912 345 678")).isEqualTo("+84912345678");
        assertThat(Normalizer.phone("84912345678")).isEqualTo("+84912345678");
        assertThat(Normalizer.phone("+84.912.345.678")).isEqualTo("+84912345678");
        assertThat(Normalizer.phone(" ")).isNull();
    }

    @Test
    void textCollapsesWhitespace() {
        assertThat(Normalizer.text("  Nguyen   Van  A ")).isEqualTo("Nguyen Van A");
    }
}
