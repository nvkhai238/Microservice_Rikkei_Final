package com.ecommerce.payment.refund;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.payment.PaymentErrorCode;
import com.ecommerce.payment.payment.Payment;
import com.ecommerce.payment.payment.PaymentEvents;
import com.ecommerce.payment.payment.PaymentRepository;
import com.ecommerce.payment.payment.PaymentTransaction;
import com.ecommerce.payment.payment.PaymentTransactionRepository;
import com.ecommerce.payment.provider.PaymentProvider;
import com.ecommerce.payment.provider.PaymentProviders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Hoàn tiền (FR-PAY-014..017, BR-009) theo 3 bước để an toàn khi crash giữa chừng:
 * <ol>
 *   <li>{@link #createPending}: (transaction) kiểm tra hạn mức + tạo Refund PENDING. Commit.</li>
 *   <li>{@link #execute}: gọi provider NGOÀI transaction (không giữ khóa DB khi chờ mạng),
 *       kèm idempotency key => gọi lại không hoàn 2 lần.</li>
 *   <li>(transaction) ghi kết quả + bút toán REFUND + event RefundCompleted/RefundFailed qua Outbox.</li>
 * </ol>
 * Crash sau bước 1: RefundRecoveryJob thấy Refund PENDING "kẹt" và chạy lại bước 2-3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refunds;
    private final PaymentRepository payments;
    private final PaymentTransactionRepository transactions;
    private final PaymentProviders providers;
    private final PaymentEvents events;
    private final AuditService audit;
    private final TransactionTemplate tx;
    private final Clock clock;

    /**
     * @return id của refund (mới hoặc đã có với cùng idempotencyKey). Null nếu không cần làm gì.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID createPending(UUID paymentId, BigDecimal requested, String reason, String idempotencyKey,
                              String actor) {
        var existing = refunds.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Refund r = existing.get();
            if (r.getStatus() == RefundStatus.FAILED) {
                // Gửi lại cùng khóa sau khi thất bại = yêu cầu thử lại (vd: nhân viên bấm retry Saga)
                r.retry(clock.instant());
                payments.lockById(r.getPaymentId()).ifPresent(p -> p.markRefundPending(clock.instant()));
                return r.getId();
            }
            return r.getStatus() == RefundStatus.PENDING ? r.getId() : null;
        }
        Payment payment = payments.lockById(paymentId).orElseThrow(() -> AppException.notFound("Payment"));
        if (!payment.getStatus().isCaptured()) {
            throw new AppException(PaymentErrorCode.PAYMENT_NOT_REFUNDABLE);
        }
        BigDecimal amount = requested.setScale(2, RoundingMode.HALF_UP);
        BigDecimal alreadyCommitted = payment.getRefundedAmount().add(refunds.sumPending(paymentId));
        if (amount.signum() <= 0 || alreadyCommitted.add(amount).compareTo(payment.getCapturedAmount()) > 0) {
            throw new AppException(PaymentErrorCode.REFUND_EXCEEDS_CAPTURED,
                    "Refundable amount left: " + payment.getCapturedAmount().subtract(alreadyCommitted));
        }
        Instant now = clock.instant();
        Refund refund = refunds.save(new Refund(paymentId, payment.getOrderId(), amount, payment.getCurrency(),
                reason, idempotencyKey, actor, now));
        payment.markRefundPending(now);
        audit.record(actor, "REFUND_REQUESTED", "PAYMENT", paymentId, null,
                Map.of("refundId", refund.getId(), "amount", amount, "reason", reason, "key", idempotencyKey),
                "SUCCESS");
        return refund.getId();
    }

    /** Gọi provider rồi ghi kết quả. Gọi nhiều lần an toàn: refund không còn PENDING thì bỏ qua. */
    public void execute(UUID refundId) {
        Refund refund = refunds.findById(refundId).orElse(null);
        if (refund == null || refund.getStatus() != RefundStatus.PENDING) {
            return;
        }
        Payment payment = payments.findById(refund.getPaymentId()).orElseThrow();
        PaymentProvider.RefundResult result;
        try {
            result = providers.get(payment.getProvider()).refund(payment.getProviderRef(), refund.getAmount(),
                    refund.getCurrency(), refund.getIdempotencyKey());
        } catch (RuntimeException ex) {
            // Lỗi tạm thời (mạng...): để PENDING, job recovery sẽ thử lại (FR-SAGA-006)
            log.warn("Refund {} provider call failed, will retry: {}", refundId, ex.getMessage());
            tx.executeWithoutResult(s -> refunds.lockById(refundId).ifPresent(r -> r.attempted(clock.instant())));
            return;
        }
        tx.executeWithoutResult(s -> finish(refundId, result));
    }

    private void finish(UUID refundId, PaymentProvider.RefundResult result) {
        Refund refund = refunds.lockById(refundId).orElseThrow();
        if (refund.getStatus() != RefundStatus.PENDING) {
            return;
        }
        Payment payment = payments.lockById(refund.getPaymentId()).orElseThrow();
        Instant now = clock.instant();
        refund.attempted(now);
        if (result.succeeded()) {
            refund.succeeded(result.providerRefundRef(), now);
            payment.applyRefund(refund.getAmount(), now);
            transactions.save(new PaymentTransaction(payment.getId(), "REFUND", refund.getAmount(),
                    refund.getCurrency(), result.providerRefundRef(), refund.getId(), refund.getReason(), now));
            events.refundCompleted(payment, refund);
            log.info("Refund {} of {} {} succeeded for payment {}", refund.getId(), refund.getAmount(),
                    refund.getCurrency(), payment.getId());
        } else {
            refund.failed(result.failureReason(), now);
            payment.refundFailed(now);
            events.refundFailed(payment, refund);
            log.error("[ALERT] Refund {} failed for payment {}: {}", refund.getId(), payment.getId(),
                    result.failureReason());
        }
        audit.record("SYSTEM", result.succeeded() ? "REFUND_SUCCEEDED" : "REFUND_FAILED", "PAYMENT",
                payment.getId(), null, Map.of("refundId", refund.getId(), "amount", refund.getAmount()),
                result.succeeded() ? "SUCCESS" : "FAILURE");
    }
}
