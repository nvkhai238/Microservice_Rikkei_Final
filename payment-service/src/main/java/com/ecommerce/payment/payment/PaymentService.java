package com.ecommerce.payment.payment;

import com.ecommerce.common.event.payload.OrderCancelledPayload;
import com.ecommerce.common.event.payload.OrderCreatedPayload;
import com.ecommerce.payment.provider.PaymentProvider;
import com.ecommerce.payment.provider.PaymentProviders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Vòng đời payment intent (FR-PAY-001..006, FR-PAY-010..012).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository payments;
    private final PaymentTransactionRepository transactions;
    private final PaymentProviders providers;
    private final PaymentEvents events;
    private final Clock clock;

    /**
     * Nghe OrderCreated: tạo intent với số tiền LẤY TỪ EVENT của order (FR-PAY-002), không bao giờ từ client.
     * Idempotent theo đơn (FR-PAY-003): đã có intent cho đơn này => không tạo thêm (unique order_id ở DB).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createForOrder(OrderCreatedPayload order) {
        if (payments.findByOrderId(order.orderId()).isPresent()) {
            return;
        }
        String providerCode = providers.supports(order.paymentMethod()) ? order.paymentMethod() : "MOCK";
        Instant now = clock.instant();
        Payment payment = new Payment(order.orderId(), order.orderCode(), order.customerId(), order.amount(),
                order.currency(), providerCode, order.reservationExpiresAt(), now);
        PaymentProvider provider = providers.get(providerCode);
        PaymentProvider.CheckoutSession session = provider.createCheckout(order.orderId(), order.orderCode(),
                order.amount(), order.currency());
        payment.attachCheckout(session.providerRef(), session.checkoutUrl(), now);
        payments.save(payment);
        log.info("Payment intent {} created for order {} ({} {})", payment.getId(), order.orderCode(),
                order.amount(), order.currency());
    }

    /** Nghe OrderCancelled: intent còn PENDING thì đóng lại, không cho thanh toán đơn đã hủy. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void closeForCancelledOrder(OrderCancelledPayload cancelled) {
        payments.lockByOrderId(cancelled.orderId()).ifPresent(p -> {
            if (p.getStatus() != PaymentStatus.PENDING) {
                return;
            }
            PaymentStatus status = "RESERVATION_EXPIRED".equals(cancelled.reason()) ? PaymentStatus.EXPIRED
                    : PaymentStatus.CANCELLED;
            p.close(status, cancelled.reason(), clock.instant());
            if (p.getProviderRef() != null) {
                providers.get(p.getProvider()).cancelCheckout(p.getProviderRef());
            }
        });
    }

    /**
     * Webhook báo THÀNH CÔNG đã được xác minh chữ ký. Đối chiếu tham chiếu đơn + số tiền + tiền tệ (FR-PAY-010)
     * rồi mới ghi nhận. @return mã kết quả để lưu vào webhook_events
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String recordSuccess(PaymentProvider.WebhookEvent event) {
        Payment p = payments.lockByProviderRef(event.providerRef()).orElse(null);
        if (p == null) {
            return "UNKNOWN_PAYMENT";
        }
        if (!p.getOrderId().equals(event.orderRef()) || p.getAmount().compareTo(event.amount()) != 0
                || !p.getCurrency().equals(event.currency())) {
            log.error("[ALERT] Webhook mismatch for payment {}: order {} amount {} {} vs expected {} {} {}",
                    p.getId(), event.orderRef(), event.amount(), event.currency(), p.getOrderId(), p.getAmount(),
                    p.getCurrency());
            return "REJECTED_MISMATCH";
        }
        if (p.getStatus().isCaptured()) {
            return "ALREADY_CAPTURED";
        }
        Instant now = clock.instant();
        // Kể cả khi intent đã EXPIRED/CANCELLED: tiền ĐÃ bị trừ ở provider => phải ghi nhận,
        // order-service sẽ thấy đơn đã hủy và yêu cầu hoàn tiền (compensation).
        p.markSucceeded(event.amount(), now);
        transactions.save(new PaymentTransaction(p.getId(), "CAPTURE", event.amount(), event.currency(),
                event.providerRef(), null, "Webhook " + event.eventId(), now));
        events.succeeded(p, now);
        return "CAPTURED";
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String recordFailure(PaymentProvider.WebhookEvent event) {
        Payment p = payments.lockByProviderRef(event.providerRef()).orElse(null);
        if (p == null) {
            return "UNKNOWN_PAYMENT";
        }
        if (p.getStatus() != PaymentStatus.PENDING) {
            return "IGNORED_" + p.getStatus();
        }
        Instant now = clock.instant();
        String category = event.failureCode() == null ? "DECLINED" : event.failureCode();
        p.markFailed(category, now);
        transactions.save(new PaymentTransaction(p.getId(), "FAILURE", event.amount(), event.currency(),
                event.providerRef(), null, category, now));
        events.failed(p, category, now);
        return "FAILED";
    }
}
