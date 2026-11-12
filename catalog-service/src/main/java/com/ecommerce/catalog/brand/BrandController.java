package com.ecommerce.catalog.brand;

import com.ecommerce.catalog.category.CategoryDtos;
import com.ecommerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BrandController {

    private final BrandService service;

    @GetMapping("/api/v1/brands")
    public ApiResponse<List<BrandDtos.BrandResponse>> listActive() {
        return ApiResponse.success(service.listActive());
    }

    @GetMapping("/api/v1/admin/catalog/brands")
    public ApiResponse<List<BrandDtos.BrandResponse>> listAll() {
        return ApiResponse.success(service.listAll());
    }

    @PostMapping("/api/v1/admin/catalog/brands")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BrandDtos.BrandResponse> create(@Valid @RequestBody BrandDtos.BrandRequest req) {
        return ApiResponse.success(service.create(req));
    }

    @PutMapping("/api/v1/admin/catalog/brands/{id}")
    public ApiResponse<BrandDtos.BrandResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody BrandDtos.BrandRequest req) {
        return ApiResponse.success(service.update(id, req));
    }

    @PatchMapping("/api/v1/admin/catalog/brands/{id}/status")
    public ApiResponse<BrandDtos.BrandResponse> status(@PathVariable UUID id,
                                                       @Valid @RequestBody CategoryDtos.StatusRequest req) {
        return ApiResponse.success(service.changeStatus(id, req.status()));
    }

    @DeleteMapping("/api/v1/admin/catalog/brands/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.success(null, "Brand deleted");
    }
}
