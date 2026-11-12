package com.ecommerce.identity.config;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.identity.user.Normalizer;
import com.ecommerce.identity.user.User;
import com.ecommerce.identity.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * "Con gà - quả trứng": muốn gán role ADMIN thì phải có ADMIN. Lần đầu khởi động, tạo admin từ
 * ADMIN_EMAIL/ADMIN_PASSWORD trong .env (không ghi cứng trong code - CON-09). Đã có thì bỏ qua.
 */
@Slf4j
@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder, AuditService audit,
                          TransactionTemplate tx, Clock clock,
                          @Value("${app.bootstrap.admin-email:}") String email,
                          @Value("${app.bootstrap.admin-password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.tx = tx;
        this.clock = clock;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set, skipping admin bootstrap");
            return;
        }
        String normalized = Normalizer.email(email);
        tx.executeWithoutResult(status -> {
            if (users.existsByEmail(normalized)) {
                return;
            }
            Instant now = clock.instant();
            User admin = User.register(normalized, passwordEncoder.encode(password), "System Administrator", null,
                    false, now);
            admin.grantAdminBootstrap(now);
            users.save(admin);
            audit.record("SYSTEM", "ADMIN_BOOTSTRAPPED", "USER", admin.getId(), null,
                    Map.of("email", normalized), "SUCCESS");
            log.info("Bootstrapped admin account {}", normalized);
        });
    }
}
