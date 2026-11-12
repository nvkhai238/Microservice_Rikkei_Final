package com.ecommerce.catalog.product;

import com.ecommerce.catalog.sku.Sku;
import com.ecommerce.catalog.sku.SkuStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProductDtos {
    private ProductDtos() {
    }

    // ---------- Request (admin) ----------
    public record ProductCreateRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 120) String slug,
            @Size(max = 10000) String description,
            @NotNull UUID categoryId,
            UUID brandId,
            @Valid @Size(max = 50) List<SkuCreateRequest> skus) {
    }

    public record ProductUpdateRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 120) String slug,
            @Size(max = 10000) String description,
            @NotNull UUID categoryId,
            UUID brandId) {
    }

    public record ProductStatusRequest(@NotNull ProductStatus status) {
    }

    public record SkuCreateRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{1,63}$") String skuCode,
            @Size(max = 200) String variantName,
            Map<String, String> attributes,
            @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal price,
            @Pattern(regexp = "^VND$", message = "Only VND is supported in v1 (A-02)") String currency,
            @Min(1) @Max(1000) Integer maxPerOrder) {
    }

    public record SkuUpdateRequest(@Size(max = 200) String variantName, Map<String, String> attributes,
                                   @NotNull @Min(1) @Max(1000) Integer maxPerOrder) {
    }

    /** Đổi giá bắt buộc có lý do để audit (FR-CAT-011, BR-014) */
    public record PriceChangeRequest(@NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal price,
                                     @NotBlank @Size(max = 500) String reason) {
    }

    public record SkuStatusRequest(@NotNull SkuStatus status) {
    }

    // ---------- Response ----------
    public record SkuResponse(UUID id, String skuCode, String variantName, Map<String, String> attributes,
                              BigDecimal price, String currency, SkuStatus status, int maxPerOrder,
                              long availableQuantity) {
        public static SkuResponse from(Sku s) {
            return new SkuResponse(s.getId(), s.getSkuCode(), s.getVariantName(), s.getAttributes(), s.getPrice(),
                    s.getCurrency(), s.getStatus(), s.getMaxPerOrder(), s.getAvailableQuantity());
        }
    }

    public record ImageResponse(UUID id, String url, UUID skuId, boolean primary, String contentType, long sizeBytes) {
    }

    public record ProductAdminResponse(UUID id, String name, String slug, String description, UUID categoryId,
                                       UUID brandId, ProductStatus status, long version, List<SkuResponse> skus,
                                       List<ImageResponse> images, Instant createdAt, Instant updatedAt) {
    }

    public record ProductListItem(UUID id, String name, String slug, ProductStatus status, UUID categoryId,
                                  UUID brandId, Instant updatedAt) {
        public static ProductListItem from(Product p) {
            return new ProductListItem(p.getId(), p.getName(), p.getSlug(), p.getStatus(), p.getCategoryId(),
                    p.getBrandId(), p.getUpdatedAt());
        }
    }

    // ---------- Response (public) ----------
    public record Ref(UUID id, String name, String slug) {
    }

    public record PublicSku(UUID id, String skuCode, String variantName, Map<String, String> attributes,
                            BigDecimal price, String currency, int maxPerOrder, boolean inStock) {
    }

    public record ProductDetail(UUID id, String name, String slug, String description, Ref category, Ref brand,
                                BigDecimal minPrice, BigDecimal maxPrice, String currency, boolean inStock,
                                List<PublicSku> skus, List<ImageResponse> images, Instant updatedAt) {
    }

    public record ProductSummary(UUID id, String name, String slug, String brandName, String categoryName,
                                 BigDecimal minPrice, BigDecimal maxPrice, String currency, boolean inStock,
                                 String imageUrl, Instant createdAt, Instant updatedAt) {
    }
}
