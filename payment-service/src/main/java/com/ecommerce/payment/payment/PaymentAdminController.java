package com.ecommerce.payment.payment;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.response.PageRequests;
import com.ecommerce.common.response.PageResponse;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.payment.reconciliation.ReconciliationService;
import com.ecommerce.payment.refund.RefundRepository;
import com.ecommerce.payment.refund.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** Nhân viên tài chính: xem giao dịch, hoàn tiền, đối soát (mục 2.3, FR-PAY-014, FR-PAY-018). */
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentRepository payments;
    private final PaymentTransactionRepository transactions;
    private final RefundRepository refunds;
    private final RefundService refundService;
    private final ReconciliationService reconciliation;
    private final AuditService audit;
    private final TransactionTemplate tx;

    @GetMapping
    public ApiResponse<PageResponse<PaymentDtos.PaymentSummary>> search(
            @RequestParam(required = false) PaymentStatus status, @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.success(PageResponse.of(payments.search(status, orderId,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))), PaymentDtos.PaymentSummary::from));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentDtos.PaymentView> get(@PathVariable UUID paymentId) {
        return ApiResponse.success(tx.execute(s -> {
            Payment p = payments.findById(paymentId).orElseThrow(() -> AppException.notFound("Payment"));
            return PaymentDtos.PaymentView.from(p, transactions.findByPaymentIdOrderByCreatedAtAsc(paymentId),
                    refunds.findByPaymentIdOrderByCreatedAtAsc(paymentId));
        }));
    }

    /**
     * Hoàn tiền toàn phần/một phần (FR-PAY-014). Idempotency-Key bắt buộc: bấm 2 lần chỉ hoàn 1 lần (FR-PAY-016).
     */
    @PostMapping("/{paymentId}/refunds")
    public ApiResponse<PaymentDtos.RefundView> refund(@PathVariable UUID paymentId,
                                                      @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                      @Valid @RequestBody PaymentDtos.RefundRequest request) {
        if (!idempotencyKey.matches("^[A-Za-z0-9_-]{8,100}$")) {
            throw AppException.validation("Idempotency-Key", "REQUIRED_8_TO_100_CHARS");
        }
        String key = "manual:" + idempotencyKey;
        UUID refundId = tx.execute(s -> refundService.createPending(paymentId, request.amount(), request.reason(),
                key, CurrentUser.actorOrSystem()));
        if (refundId != null) {
            refundService.execute(refundId);
        }
        var refund = refunds.findByIdempotencyKey(key).orElseThrow();
        return ApiResponse.success(PaymentDtos.RefundView.from(refund));
    }

    @PostMapping("/refunds/{refundId}/retry")
    public ApiResponse<PaymentDtos.RefundView> retryRefund(@PathVariable UUID refundId) {
        var refund = refunds.findById(refundId).orElseThrow(() -> AppException.notFound("Refund"));
        UUID id = tx.execute(s -> {
            UUID result = refundService.createPending(refund.getPaymentId(), refund.getAmount(), refund.getReason(),
                    refund.getIdempotencyKey(), CurrentUser.actorOrSystem());
            audit.record("REFUND_MANUAL_RETRY", "PAYMENT", refund.getPaymentId(), null, Map.of("refundId", refundId));
            return result;
        });
        if (id != null) {
            refundService.execute(id);
        }
        return ApiResponse.success(PaymentDtos.RefundView.from(refunds.findById(refundId).orElseThrow()));
    }

    @PostMapping("/reconciliation/runs")
    public ApiResponse<ReconciliationService.RunView> runReconciliation() {
        return ApiResponse.success(reconciliation.run(CurrentUser.actorOrSystem()));
    }

    @GetMapping("/reconciliation/runs")
    public ApiResponse<java.util.List<ReconciliationService.RunView>> runs() {
        return ApiResponse.success(reconciliation.recentRuns());
    }

    @GetMapping("/reconciliation/runs/{runId}")
    public ApiResponse<ReconciliationService.RunDetail> run(@PathVariable UUID runId) {
        return ApiResponse.success(reconciliation.detail(runId));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditService.AuditEntry>> auditLogs(@RequestParam(required = false) String targetId,
                                                                        @RequestParam(required = false) Integer page,
                                                                        @RequestParam(required = false) Integer size) {
        return ApiResponse.success(audit.search("PAYMENT", targetId, null,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }
}
