package com.ecommerce.payment.payment;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.payment.refund.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Khách xem trạng thái thanh toán + link thanh toán của đơn mình (UI-05, UI-06). */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository payments;
    private final PaymentTransactionRepository transactions;
    private final RefundRepository refunds;

    /** Payment intent được tạo BẤT ĐỒNG BỘ sau khi đặt đơn: 404 trong vài trăm ms đầu là bình thường, client poll lại. */
    @GetMapping("/api/v1/payments/orders/{orderId}")
    @Transactional(readOnly = true)
    public ApiResponse<PaymentDtos.PaymentView> byOrder(@PathVariable UUID orderId) {
        Payment p = payments.findByOrderId(orderId)
                .filter(x -> x.getCustomerId().equals(CurrentUser.id()))
                .orElseThrow(() -> AppException.notFound("Payment"));
        return ApiResponse.success(PaymentDtos.PaymentView.from(p,
                transactions.findByPaymentIdOrderByCreatedAtAsc(p.getId()),
                refunds.findByPaymentIdOrderByCreatedAtAsc(p.getId())));
    }
}
