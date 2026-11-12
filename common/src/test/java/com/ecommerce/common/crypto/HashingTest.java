package com.ecommerce.common.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashingTest {

    @Test
    void sha256IsDeterministicHex() {
        assertThat(Hashing.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void randomTokensAreUrlSafeAndUnique() {
        String a = Hashing.randomToken(32);
        String b = Hashing.randomToken(32);
        assertThat(a).isNotEqualTo(b).matches("^[A-Za-z0-9_-]{43}$");
    }

    @Test
    void constantTimeEquals() {
        assertThat(Hashing.constantTimeEquals("abc", "abc")).isTrue();
        assertThat(Hashing.constantTimeEquals("abc", "abd")).isFalse();
        assertThat(Hashing.constantTimeEquals(null, "abc")).isFalse();
    }
}
