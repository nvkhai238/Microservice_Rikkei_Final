package com.ecommerce.identity.user;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void locksAfterMaxFailedLoginsAndUnlocksLater() {
        User user = User.register("a@b.com", "hash", "A B", null, false, now);
        for (int i = 0; i < 4; i++) {
            assertThat(user.recordFailedLogin(5, Duration.ofMinutes(15), now)).isFalse();
        }
        assertThat(user.recordFailedLogin(5, Duration.ofMinutes(15), now)).isTrue();
        assertThat(user.isLocked(now.plusSeconds(60))).isTrue();
        assertThat(user.isLocked(now.plus(Duration.ofMinutes(16)))).isFalse();
    }

    @Test
    void successfulLoginResetsCounter() {
        User user = User.register("a@b.com", "hash", "A B", null, false, now);
        user.recordFailedLogin(5, Duration.ofMinutes(15), now);
        user.recordSuccessfulLogin(now);
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void newUserIsCustomerAndPendingWhenVerificationRequired() {
        User user = User.register("a@b.com", "hash", "A B", null, true, now);
        assertThat(user.getRoles()).containsExactly("CUSTOMER");
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        user.markEmailVerified(now);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
