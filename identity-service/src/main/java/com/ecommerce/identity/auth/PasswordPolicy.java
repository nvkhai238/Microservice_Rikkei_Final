package com.ecommerce.identity.auth;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.CommonErrorCode;
import com.ecommerce.common.exception.FieldErrorDetail;
import com.ecommerce.identity.config.AuthProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chính sách mật khẩu cấu hình được (FR-IAM-003): độ dài, có chữ, có số, không chứa phần đầu email.
 * Trả về TẤT CẢ lý do vi phạm một lần để người dùng sửa một lượt.
 */
@Component
public class PasswordPolicy {

    private final AuthProperties.Password config;

    public PasswordPolicy(AuthProperties props) {
        this.config = props.password();
    }

    public void validate(String password, String email, String field) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        if (password == null || password.length() < config.minLength()) {
            errors.add(new FieldErrorDetail(field, "PASSWORD_TOO_SHORT"));
        }
        if (password != null && password.getBytes(StandardCharsets.UTF_8).length > config.maxLength()) {
            errors.add(new FieldErrorDetail(field, "PASSWORD_TOO_LONG"));
        }
        if (password != null && password.chars().noneMatch(Character::isLetter)) {
            errors.add(new FieldErrorDetail(field, "PASSWORD_REQUIRES_LETTER"));
        }
        if (password != null && password.chars().noneMatch(Character::isDigit)) {
            errors.add(new FieldErrorDetail(field, "PASSWORD_REQUIRES_DIGIT"));
        }
        if (password != null && email != null) {
            String localPart = email.split("@")[0].toLowerCase(Locale.ROOT);
            if (localPart.length() >= 4 && password.toLowerCase(Locale.ROOT).contains(localPart)) {
                errors.add(new FieldErrorDetail(field, "PASSWORD_CONTAINS_EMAIL"));
            }
        }
        if (!errors.isEmpty()) {
            throw new AppException(CommonErrorCode.VALIDATION_ERROR, "Password does not meet the policy", errors);
        }
    }
}
