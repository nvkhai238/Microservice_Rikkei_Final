package com.ecommerce.order.admin;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.response.PageRequests;
import com.ecommerce.common.response.PageResponse;
import com.ecommerce.order.order.OrderDtos;
import com.ecommerce.order.order.OrderLifecycleService;
import com.ecommerce.order.order.OrderQueryService;
import com.ecommerce.order.order.OrderStatus;
import com.ecommerce.order.saga.SagaInstance;
import com.ecommerce.order.saga.SagaInstanceRepository;
import com.ecommerce.order.saga.SagaLog;
import com.ecommerce.order.saga.SagaStatus;
import com.ecommerce.order.saga.SagaStep;
import com.ecommerce.order.saga.SagaStepRepository;
import com.ecommerce.order.saga.SagaType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Vận hành đơn cho ORDER_STAFF / ADMIN (FR-ORD-013, FR-ORD-014, FR-SAGA-010). */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderQueryService queryService;
    private final OrderLifecycleService lifecycleService;
    private final SagaInstanceRepository sagas;
    private final SagaStepRepository sagaSteps;
    private final SagaLog sagaLog;
    private final AuditService audit;
    private final TransactionTemplate tx;

    @GetMapping
    public ApiResponse<PageResponse<OrderDtos.OrderSummary>> search(
            @RequestParam(required = false) String code, @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.success(queryService.search(code, customerId, status, from, to,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")))));
    }

    @GetMapping("/{idOrCode}")
    public ApiResponse<OrderDtos.OrderView> get(@PathVariable String idOrCode) {
        return ApiResponse.success(queryService.anyOrder(idOrCode));
    }

    @PatchMapping("/{idOrCode}/status")
    public ApiResponse<OrderDtos.OrderView> changeStatus(@PathVariable String idOrCode,
                                                         @Valid @RequestBody OrderDtos.StatusChangeRequest req) {
        return ApiResponse.success(lifecycleService.staffTransition(queryService.resolveId(idOrCode), req.status(),
                req.reason()));
    }

    @PostMapping("/{idOrCode}/cancel")
    public ApiResponse<OrderDtos.OrderView> cancel(@PathVariable String idOrCode,
                                                   @Valid @RequestBody OrderDtos.CancelRequest req) {
        return ApiResponse.success(lifecycleService.cancel(queryService.resolveId(idOrCode), null, req.reason()));
    }

    // ---------------- Saga (FR-SAGA-010) ----------------
    @GetMapping("/sagas")
    public ApiResponse<PageResponse<SagaView>> sagas(@RequestParam(required = false) SagaStatus status,
                                                     @RequestParam(required = false) SagaType type,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        return ApiResponse.success(PageResponse.of(sagas.search(status, type,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))), s -> SagaView.from(s, null)));
    }

    @GetMapping("/sagas/{sagaId}")
    public ApiResponse<SagaView> saga(@PathVariable UUID sagaId) {
        SagaInstance saga = sagas.findById(sagaId).orElseThrow(() -> AppException.notFound("Saga"));
        return ApiResponse.success(SagaView.from(saga, sagaSteps.findBySagaIdOrderByCreatedAtAsc(sagaId)));
    }

    /** Retry Saga lỗi: thao tác thủ công => BẮT BUỘC audit (FR-SAGA-010). */
    @PostMapping("/sagas/{sagaId}/retry")
    public ApiResponse<Map<String, String>> retry(@PathVariable UUID sagaId) {
        String result = tx.execute(status -> {
            SagaInstance saga = sagas.findById(sagaId).orElseThrow(() -> AppException.notFound("Saga"));
            if (saga.getStatus() != SagaStatus.FAILED && saga.getStatus() != SagaStatus.AWAITING_REFUND) {
                throw AppException.businessRule("SAGA_NOT_RETRYABLE", "Only FAILED or AWAITING_REFUND sagas can be retried");
            }
            String outcome = lifecycleService.retryRefund(saga.getOrderId());
            sagaLog.retried(saga, outcome);
            audit.record("SAGA_MANUAL_RETRY", "SAGA", sagaId, Map.of("status", saga.getStatus()),
                    Map.of("outcome", outcome));
            return outcome;
        });
        return ApiResponse.success(Map.of("result", result == null ? "" : result));
    }

    public record SagaStepView(String step, String status, String detail, String actor, Instant at) {
    }

    public record SagaView(UUID id, UUID orderId, SagaType type, SagaStatus status, String currentStep,
                           String correlationId, String lastError, int retryCount, Instant createdAt,
                           Instant updatedAt, List<SagaStepView> steps) {
        static SagaView from(SagaInstance s, List<SagaStep> steps) {
            return new SagaView(s.getId(), s.getOrderId(), s.getType(), s.getStatus(), s.getCurrentStep(),
                    s.getCorrelationId(), s.getLastError(), s.getRetryCount(), s.getCreatedAt(), s.getUpdatedAt(),
                    steps == null ? null : steps.stream().map(st -> new SagaStepView(st.getStep(), st.getStatus(),
                            st.getDetail(), st.getActor(), st.getCreatedAt())).toList());
        }
    }
}
