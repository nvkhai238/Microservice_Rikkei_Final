package com.ecommerce.payment.payment;

import com.ecommerce.payment.refund.Refund;
import com.ecommerce.payment.refund.RefundStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PaymentDtos {
    private PaymentDtos() {
    }

    public record TransactionView(UUID id, String type, BigDecimal amount, String currency, String providerRef,
                                  String note, Instant createdAt) {
        static TransactionView from(PaymentTransaction t) {
            return new TransactionView(t.getId(), t.getType(), t.getAmount(), t.getCurrency(), t.getProviderRef(),
                    t.getNote(), t.getCreatedAt());
        }
    }

    public record RefundView(UUID id, BigDecimal amount, String currency, String reason, RefundStatus status,
                             String requestedBy, String providerRef, String failureReason, int attempts,
                             Instant createdAt) {
        public static RefundView from(Refund r) {
            return new RefundView(r.getId(), r.getAmount(), r.getCurrency(), r.getReason(), r.getStatus(),
                    r.getRequestedBy(), r.getProviderRef(), r.getFailureReason(), r.getAttempts(), r.getCreatedAt());
        }
    }

    /**
     * UI-05: client hiển thị PENDING (đang chờ) / SUCCEEDED / FAILED / EXPIRED theo status ở đây,
     * KHÔNG theo việc trình duyệt được redirect về.
     */
    public record PaymentView(UUID id, UUID orderId, String orderCode, BigDecimal amount, String currency,
                              String provider, PaymentStatus status, String checkoutUrl, Instant expiresAt,
                              BigDecimal capturedAmount, BigDecimal refundedAmount, String failureReason,
                              List<TransactionView> transactions, List<RefundView> refunds, Instant createdAt) {
        static PaymentView from(Payment p, List<PaymentTransaction> txs, List<Refund> refunds) {
            return new PaymentView(p.getId(), p.getOrderId(), p.getOrderCode(), p.getAmount(), p.getCurrency(),
                    p.getProvider(), p.getStatus(), p.getStatus() == PaymentStatus.PENDING ? p.getCheckoutUrl() : null,
                    p.getExpiresAt(), p.getCapturedAmount(), p.getRefundedAmount(), p.getFailureReason(),
                    txs.stream().map(TransactionView::from).toList(),
                    refunds.stream().map(RefundView::from).toList(), p.getCreatedAt());
        }
    }

    public record PaymentSummary(UUID id, String orderCode, BigDecimal amount, String currency, PaymentStatus status,
                                 BigDecimal refundedAmount, Instant updatedAt) {
        static PaymentSummary from(Payment p) {
            return new PaymentSummary(p.getId(), p.getOrderCode(), p.getAmount(), p.getCurrency(), p.getStatus(),
                    p.getRefundedAmount(), p.getUpdatedAt());
        }
    }

    public record RefundRequest(@NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
                                @NotBlank @Size(max = 500) String reason) {
    }
}
