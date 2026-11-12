package com.ecommerce.catalog.category;

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
public class CategoryController {

    private final CategoryService service;

    // ---------- Public ----------
    @GetMapping("/api/v1/categories")
    public ApiResponse<List<CategoryDtos.CategoryNode>> tree() {
        return ApiResponse.success(service.activeTree());
    }

    // ---------- Admin (CATALOG_STAFF, ADMIN - xem SecurityConfig) ----------
    @GetMapping("/api/v1/admin/catalog/categories")
    public ApiResponse<List<CategoryDtos.CategoryResponse>> listAll() {
        return ApiResponse.success(service.listAll());
    }

    @GetMapping("/api/v1/admin/catalog/categories/{id}")
    public ApiResponse<CategoryDtos.CategoryResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping("/api/v1/admin/catalog/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryDtos.CategoryResponse> create(@Valid @RequestBody CategoryDtos.CategoryRequest req) {
        return ApiResponse.success(service.create(req));
    }

    @PutMapping("/api/v1/admin/catalog/categories/{id}")
    public ApiResponse<CategoryDtos.CategoryResponse> update(@PathVariable UUID id,
                                                             @Valid @RequestBody CategoryDtos.CategoryRequest req) {
        return ApiResponse.success(service.update(id, req));
    }

    @PatchMapping("/api/v1/admin/catalog/categories/{id}/status")
    public ApiResponse<CategoryDtos.CategoryResponse> status(@PathVariable UUID id,
                                                             @Valid @RequestBody CategoryDtos.StatusRequest req) {
        return ApiResponse.success(service.changeStatus(id, req.status()));
    }

    @DeleteMapping("/api/v1/admin/catalog/categories/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.success(null, "Category deleted");
    }

}
