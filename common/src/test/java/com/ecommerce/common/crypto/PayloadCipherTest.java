package com.ecommerce.common.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayloadCipherTest {

    private final String key = Base64.getEncoder().encodeToString(new byte[32]);
    private final PayloadCipher cipher = new PayloadCipher(key);

    @Test
    void encryptsAndDecryptsRoundTrip() {
        String secret = "reset-token-abc";
        String encrypted = cipher.encrypt(secret);
        assertThat(encrypted).doesNotContain(secret);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(secret);
    }

    @Test
    void sameInputEncryptsDifferentlyBecauseOfRandomIv() {
        assertThat(cipher.encrypt("x")).isNotEqualTo(cipher.encrypt("x"));
    }

    @Test
    void tamperedCiphertextIsRejected() {
        byte[] raw = Base64.getDecoder().decode(cipher.encrypt("token"));
        raw[raw.length - 1] ^= 1;   // lật 1 bit
        String tampered = Base64.getEncoder().encodeToString(raw);
        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingKeyFailsClearly() {
        assertThatThrownBy(() -> new PayloadCipher("").encrypt("x"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("EVENT_PAYLOAD_KEY");
    }
}
