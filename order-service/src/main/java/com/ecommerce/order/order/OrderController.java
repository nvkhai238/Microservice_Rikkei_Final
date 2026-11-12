package com.ecommerce.order.order;

import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.response.PageRequests;
import com.ecommerce.common.response.PageResponse;
import com.ecommerce.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API đơn hàng cho khách (UC-03, UC-06, UI-06). */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderPlacementService placementService;
    private final OrderQueryService queryService;
    private final OrderLifecycleService lifecycleService;

    /**
     * Tạo đơn. Header Idempotency-Key BẮT BUỘC (FR-ORD-002): client sinh 1 UUID cho mỗi lần bấm "Đặt hàng"
     * và GIỮ NGUYÊN khi retry. 201 = tạo mới, 200 = trả lại đơn đã tạo trước đó với cùng khóa.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDtos.OrderView>> place(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderDtos.PlaceOrderRequest request) {
        var result = placementService.place(CurrentUser.id(), idempotencyKey, request, CurrentUser.bearerToken());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success(result.order(), result.created()
                        ? "Order created, awaiting payment" : "Order already created for this Idempotency-Key"));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderDtos.OrderSummary>> myOrders(@RequestParam(required = false) Integer page,
                                                                      @RequestParam(required = false) Integer size) {
        return ApiResponse.success(queryService.myOrders(CurrentUser.id(),
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")))));
    }

    @GetMapping("/{idOrCode}")
    public ApiResponse<OrderDtos.OrderView> get(@PathVariable String idOrCode) {
        return ApiResponse.success(queryService.myOrder(CurrentUser.id(), idOrCode));
    }

    @PostMapping("/{idOrCode}/cancel")
    public ApiResponse<OrderDtos.OrderView> cancel(@PathVariable String idOrCode,
                                                   @Valid @RequestBody OrderDtos.CancelRequest request) {
        var view = queryService.myOrder(CurrentUser.id(), idOrCode);
        return ApiResponse.success(lifecycleService.cancel(view.id(), CurrentUser.id(), request.reason()));
    }
}
