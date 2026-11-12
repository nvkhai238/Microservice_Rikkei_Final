package com.ecommerce.payment.refund;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Refund PENDING quá 1 phút (crash, provider tạm lỗi...) được thử lại. Idempotency key giữ cho an toàn. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundRecoveryJob {

    private final RefundRepository refunds;
    private final RefundService refundService;
    private final TransactionTemplate tx;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.payment.refund-recovery-interval:60s}", initialDelayString = "30s")
    public void recover() {
        List<UUID> stuck = tx.execute(s -> refunds.lockStuckPending(clock.instant().minus(Duration.ofMinutes(1))));
        if (stuck == null || stuck.isEmpty()) {
            return;
        }
        log.info("Retrying {} stuck refund(s)", stuck.size());
        stuck.forEach(refundService::execute);
    }
}
