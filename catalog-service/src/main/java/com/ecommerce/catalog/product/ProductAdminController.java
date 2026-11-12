package com.ecommerce.catalog.product;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.response.PageRequests;
import com.ecommerce.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** Quản trị sản phẩm/SKU/giá cho CATALOG_STAFF, ADMIN (quyền ở SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/catalog")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductAdminService service;
    private final SearchIndexer searchIndexer;
    private final AuditService auditService;

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductDtos.ProductListItem>> search(
            @RequestParam(required = false) String q, @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.success(service.search(q, status, categoryId, brandId,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by("id")))));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductDtos.ProductAdminResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductDtos.ProductAdminResponse> create(@Valid @RequestBody ProductDtos.ProductCreateRequest req) {
        return ApiResponse.success(service.create(req));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<ProductDtos.ProductAdminResponse> update(@PathVariable UUID id,
                                                                @Valid @RequestBody ProductDtos.ProductUpdateRequest req) {
        return ApiResponse.success(service.update(id, req));
    }

    @PatchMapping("/products/{id}/status")
    public ApiResponse<ProductDtos.ProductAdminResponse> status(@PathVariable UUID id,
                                                                @Valid @RequestBody ProductDtos.ProductStatusRequest req) {
        return ApiResponse.success(service.changeStatus(id, req.status()));
    }

    @PostMapping("/products/{id}/skus")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductDtos.SkuResponse> addSku(@PathVariable UUID id,
                                                       @Valid @RequestBody ProductDtos.SkuCreateRequest req) {
        return ApiResponse.success(service.addSku(id, req));
    }

    @PutMapping("/skus/{skuId}")
    public ApiResponse<ProductDtos.SkuResponse> updateSku(@PathVariable UUID skuId,
                                                          @Valid @RequestBody ProductDtos.SkuUpdateRequest req) {
        return ApiResponse.success(service.updateSku(skuId, req));
    }

    @PatchMapping("/skus/{skuId}/price")
    public ApiResponse<ProductDtos.SkuResponse> changePrice(@PathVariable UUID skuId,
                                                            @Valid @RequestBody ProductDtos.PriceChangeRequest req) {
        return ApiResponse.success(service.changePrice(skuId, req));
    }

    @PatchMapping("/skus/{skuId}/status")
    public ApiResponse<ProductDtos.SkuResponse> changeSkuStatus(@PathVariable UUID skuId,
                                                                @Valid @RequestBody ProductDtos.SkuStatusRequest req) {
        return ApiResponse.success(service.changeSkuStatus(skuId, req.status()));
    }

    /** FR-CAT-018: tái lập chỉ mục tìm kiếm từ nguồn sự thật */
    @PostMapping("/search/reindex")
    public ApiResponse<Map<String, Integer>> reindex() {
        return ApiResponse.success(Map.of("reindexedProducts", searchIndexer.rebuildAll()));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditService.AuditEntry>> auditLogs(
            @RequestParam(required = false) String targetType, @RequestParam(required = false) String targetId,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.success(auditService.search(targetType, targetId, null,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

}
