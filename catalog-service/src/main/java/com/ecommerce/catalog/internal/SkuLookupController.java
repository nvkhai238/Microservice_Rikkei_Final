package com.ecommerce.catalog.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * API NỘI BỘ cho order-service: tra cứu nhiều SKU một lần (giá hiện hành, còn bán không, tên để snapshot).
 * Một lần gọi cho cả giỏ hàng thay vì N lần => nhanh hơn (NFR-PERF-004).
 */
@RestController
@RequiredArgsConstructor
public class SkuLookupController {

    private final SkuLookupService service;

    @PostMapping("/internal/v1/skus/lookup")
    public List<SkuSnapshot> lookup(@Valid @RequestBody LookupRequest request) {
        return service.lookup(request.skuIds());
    }

    public record LookupRequest(@NotEmpty @Size(max = 100) List<UUID> skuIds) {
    }
}
