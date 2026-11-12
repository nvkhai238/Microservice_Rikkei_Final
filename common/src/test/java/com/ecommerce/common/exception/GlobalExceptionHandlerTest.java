package com.ecommerce.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void appExceptionUsesItsHttpStatusAndCode() {
        var response = handler.handleApp(new AppException(CommonErrorCode.INVENTORY_INSUFFICIENT));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("INVENTORY_INSUFFICIENT");
    }

    @Test
    void unexpectedErrorsHideInternals() {
        var response = handler.handleUnexpected(new IllegalStateException("SELECT * FROM secret_table"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("secret_table");
        assertThat(response.getBody().details()).isEmpty();
    }

    @Test
    void constraintNamesBecomeStableReasonCodes() {
        assertThat(GlobalExceptionHandler.reasonOf("NotBlank")).isEqualTo("NOT_BLANK");
        assertThat(GlobalExceptionHandler.reasonOf("Size")).isEqualTo("SIZE");
        assertThat(GlobalExceptionHandler.reasonOf(null)).isEqualTo("INVALID");
    }
}
