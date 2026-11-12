package com.ecommerce.identity.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/** Xóa refresh token đã hết hạn lâu (giữ lại 30 ngày để điều tra sự cố reuse). */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final RefreshTokenRepository repository;
    private final Clock clock;

    @Scheduled(cron = "${app.auth.token-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void cleanup() {
        int deleted = repository.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(30)));
        if (deleted > 0) {
            log.info("Removed {} expired refresh token(s)", deleted);
        }
    }
}
