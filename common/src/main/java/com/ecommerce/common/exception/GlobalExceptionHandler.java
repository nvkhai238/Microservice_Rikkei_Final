package com.ecommerce.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleApp(AppException ex) {
        ErrorCode code = ex.getErrorCode();
        if (code.httpStatus().is5xxServerError()) {
            log.error("Application error {}: {}", code.code(), ex.getMessage(), ex);
        } else {
            log.debug("Application error {}: {}", code.code(), ex.getMessage());
        }
        return build(code, ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> details = ex.getBindingResult().getAllErrors().stream()
            .map(err -> err instanceof FieldError fe
                ? new FieldErrorDetail(fe.getField(), reasonOf(fe.getCode()))
                : new FieldErrorDetail(err.getObjectName(), reasonOf(err.getCode())))
            .toList();
        return build(CommonErrorCode.VALIDATION_ERROR, CommonErrorCode.VALIDATION_ERROR.defaultMessage(), details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        List<FieldErrorDetail> details = ex.getParameterValidationResults().stream()
            .flatMap(r -> r.getResolvableErrors().stream()
                .map(e -> new FieldErrorDetail(r.getMethodParameter().getParameterName(),
                    reasonOf(e.getCodes() == null || e.getCodes().length == 0 ? null
                        : lastPart(e.getCodes()[e.getCodes().length - 1])))))
            .toList();
        return build(CommonErrorCode.VALIDATION_ERROR, CommonErrorCode.VALIDATION_ERROR.defaultMessage(), details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        List<FieldErrorDetail> details = ex.getConstraintViolations().stream()
            .map(v -> new FieldErrorDetail(lastPart(v.getPropertyPath().toString()),
                reasonOf(v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())))
            .toList();
        return build(CommonErrorCode.VALIDATION_ERROR, CommonErrorCode.VALIDATION_ERROR.defaultMessage(), details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body: {}", ex.getMessage());
        return build(CommonErrorCode.VALIDATION_ERROR, "Malformed or unrecognized request body", List.of());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return build(CommonErrorCode.VALIDATION_ERROR, "Missing required header",
            List.of(new FieldErrorDetail(ex.getHeaderName(), "REQUIRED")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return build(CommonErrorCode.VALIDATION_ERROR, "Missing required parameter",
            List.of(new FieldErrorDetail(ex.getParameterName(), "REQUIRED")));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        return build(CommonErrorCode.VALIDATION_ERROR, "Missing required part",
            List.of(new FieldErrorDetail(ex.getRequestPartName(), "REQUIRED")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(CommonErrorCode.VALIDATION_ERROR, CommonErrorCode.VALIDATION_ERROR.defaultMessage(),
            List.of(new FieldErrorDetail(ex.getName(), "INVALID_FORMAT")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoRoute(NoResourceFoundException ex) {
        return build(CommonErrorCode.RESOURCE_NOT_FOUND, CommonErrorCode.RESOURCE_NOT_FOUND.defaultMessage(), List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(CommonErrorCode.METHOD_NOT_ALLOWED, CommonErrorCode.METHOD_NOT_ALLOWED.defaultMessage(), List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE, CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.defaultMessage(), List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException ex) {
        return build(CommonErrorCode.PAYLOAD_TOO_LARGE, CommonErrorCode.PAYLOAD_TOO_LARGE.defaultMessage(), List.of());
    }

    /**
     * @PreAuthorize từ chối => exception ném ra TỪ TRONG controller nên advice này bắt được trước Spring Security.
     * Nếu không khai báo ở đây, nó sẽ rơi xuống handler Exception chung và thành 500 (sai).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(CommonErrorCode.ACCESS_DENIED, CommonErrorCode.ACCESS_DENIED.defaultMessage(), List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return build(CommonErrorCode.AUTHENTICATION_REQUIRED, CommonErrorCode.AUTHENTICATION_REQUIRED.defaultMessage(), List.of());
    }

    /**
     * Hai request cùng sửa một bản ghi (optimistic lock / lock timeout): báo client thử lại (DR-008).
     */
    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleLocking(RuntimeException ex) {
        log.info("Concurrent modification: {}", ex.getMessage());
        return build(CommonErrorCode.STATE_CONFLICT, "The resource was modified concurrently, please retry", List.of());
    }

    /**
     * Vi phạm unique/foreign key... KHÔNG trả câu SQL ra ngoài.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        log.info("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(CommonErrorCode.STATE_CONFLICT, "The request conflicts with existing data", List.of());
    }

    /**
     * Lưới an toàn cuối cùng: log đầy đủ ở server, client chỉ nhận thông báo chung + correlationId.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(CommonErrorCode.INTERNAL_ERROR, CommonErrorCode.INTERNAL_ERROR.defaultMessage(), List.of());
    }

    private static ResponseEntity<ErrorResponse> build(ErrorCode code, String message, List<FieldErrorDetail> details) {
        return ResponseEntity.status(code.httpStatus()).body(ErrorResponse.of(code, message, details));
    }

    /**
     * "NotBlank" -> "NOT_BLANK", "Size" -> "SIZE"... để client có mã ổn định.
     */
    static String reasonOf(String constraintCode) {
        if (constraintCode == null || constraintCode.isBlank()) {
            return "INVALID";
        }
        return constraintCode.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    private static String lastPart(String path) {
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }
}
