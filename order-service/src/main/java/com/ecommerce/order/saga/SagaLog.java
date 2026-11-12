package com.ecommerce.order.saga;

import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.common.web.CorrelationId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Ghi nhận trạng thái + từng bước của Saga. Đây là Saga dạng ORCHESTRATION: order-service là "nhạc trưởng",
 * quyết định bước tiếp theo khi nhận kết quả từ payment; mọi bước đều được lưu lại để quan sát/retry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaLog {

    private final SagaInstanceRepository sagas;
    private final SagaStepRepository steps;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public SagaInstance start(UUID orderId, SagaType type, SagaStatus status, String step, String detail) {
        Instant now = clock.instant();
        SagaInstance saga = sagas.findByOrderIdAndType(orderId, type).orElseGet(() -> sagas.save(
                new SagaInstance(orderId, type, status, step, CorrelationId.current(), now)));
        saga.advance(status, step, now);
        steps.save(new SagaStep(saga.getId(), step, "DONE", detail, CurrentUser.actorOrSystem(), now));
        return saga;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void step(UUID orderId, SagaType type, SagaStatus status, String step, String stepStatus, String detail) {
        Instant now = clock.instant();
        SagaInstance saga = sagas.findByOrderIdAndType(orderId, type).orElseGet(() -> sagas.save(
                new SagaInstance(orderId, type, status, step, CorrelationId.current(), now)));
        saga.advance(status, step, now);
        steps.save(new SagaStep(saga.getId(), step, stepStatus, detail, CurrentUser.actorOrSystem(), now));
    }

    /** Lỗi cần can thiệp: lưu lỗi + log [ALERT] để vận hành biết (FR-SAGA-004). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void fail(UUID orderId, SagaType type, String step, String error) {
        Instant now = clock.instant();
        SagaInstance saga = sagas.findByOrderIdAndType(orderId, type).orElseGet(() -> sagas.save(
                new SagaInstance(orderId, type, SagaStatus.FAILED, step, CorrelationId.current(), now)));
        saga.fail(step, error, now);
        steps.save(new SagaStep(saga.getId(), step, "FAILED", error, CurrentUser.actorOrSystem(), now));
        log.error("[ALERT] Saga {} for order {} failed at {}: {}", type, orderId, step, error);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void retried(SagaInstance saga, String detail) {
        Instant now = clock.instant();
        saga.markRetried(now);
        steps.save(new SagaStep(saga.getId(), "MANUAL_RETRY", "INFO", detail, CurrentUser.actorOrSystem(), now));
    }
}
