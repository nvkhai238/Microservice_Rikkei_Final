package com.ecommerce.identity.auth;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.identity.config.AuthProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy(new AuthProperties(10, 5, Duration.ofMinutes(15), false,
            Duration.ofHours(24), Duration.ofMinutes(30), new AuthProperties.Password(8, 72)));

    @Test
    void acceptsStrongPassword() {
        assertThatCode(() -> policy.validate("correct-horse-42", "an@gmail.com", "password")).doesNotThrowAnyException();
    }

    @Test
    void rejectsShortAndDigitlessPasswordsWithAllReasons() {
        assertThatThrownBy(() -> policy.validate("abc", "an@gmail.com", "password"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(((AppException) e).getDetails())
                        .extracting(d -> d.reason())
                        .contains("PASSWORD_TOO_SHORT", "PASSWORD_REQUIRES_DIGIT"));
    }

    @Test
    void rejectsPasswordContainingEmailName() {
        assertThatThrownBy(() -> policy.validate("nguyenvan123", "nguyenvan@gmail.com", "password"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void rejectsPasswordLongerThanBcryptLimit() {
        assertThatThrownBy(() -> policy.validate("a1".repeat(40), "x@y.com", "password"))
                .isInstanceOf(AppException.class);
    }
}
