package com.ecommerce.payment.webhook;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** FR-PAY-007 (chữ ký), FR-PAY-008 (chống replay). */
class HmacSignatureTest {
    private static final String SECRET = "test-secret";
    private static final String BODY = "{\"id\":\"evt_1\",\"type\":\"payment.succeeded\"}";
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final Duration window = Duration.ofMinutes(5);

    @Test
    void validSignatureIsAccepted() {
        String header = HmacSignature.header(SECRET, now, BODY);
        assertThat(HmacSignature.verify(SECRET, header, BODY, now.plusSeconds(10), window))
                .isEqualTo(HmacSignature.Result.VALID);
    }

    @Test
    void modifiedBodyIsRejected() {
        String header = HmacSignature.header(SECRET, now, BODY);
        assertThat(HmacSignature.verify(SECRET, header, BODY.replace("succeeded", "failed"), now, window))
                .isEqualTo(HmacSignature.Result.INVALID);
    }

    @Test
    void wrongSecretIsRejected() {
        String header = HmacSignature.header("attacker-secret", now, BODY);
        assertThat(HmacSignature.verify(SECRET, header, BODY, now, window)).isEqualTo(HmacSignature.Result.INVALID);
    }

    @Test
    void oldTimestampIsTreatedAsReplay() {
        String header = HmacSignature.header(SECRET, now, BODY);
        assertThat(HmacSignature.verify(SECRET, header, BODY, now.plus(Duration.ofMinutes(10)), window))
                .isEqualTo(HmacSignature.Result.EXPIRED);
    }

    @Test
    void forgedTimestampBreaksSignature() {
        String header = HmacSignature.header(SECRET, now, BODY);
        String forged = header.replaceFirst("t=\\d+", "t=" + now.plusSeconds(3600).getEpochSecond());
        assertThat(HmacSignature.verify(SECRET, forged, BODY, now.plusSeconds(3600), window))
                .isEqualTo(HmacSignature.Result.INVALID);
    }

    @Test
    void missingOrMalformedHeaderIsRejected() {
        assertThat(HmacSignature.verify(SECRET, null, BODY, now, window)).isEqualTo(HmacSignature.Result.INVALID);
        assertThat(HmacSignature.verify(SECRET, "garbage", BODY, now, window)).isEqualTo(HmacSignature.Result.INVALID);
    }
}
