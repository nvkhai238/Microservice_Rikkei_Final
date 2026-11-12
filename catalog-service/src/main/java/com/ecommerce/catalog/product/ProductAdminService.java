package com.ecommerce.catalog.product;

import com.ecommerce.catalog.CatalogErrorCode;
import com.ecommerce.catalog.brand.BrandRepository;
import com.ecommerce.catalog.category.Category;
import com.ecommerce.catalog.category.CategoryRepository;
import com.ecommerce.catalog.image.ImageService;
import com.ecommerce.catalog.sku.Sku;
import com.ecommerce.catalog.sku.SkuRepository;
import com.ecommerce.catalog.sku.SkuStatus;
import com.ecommerce.catalog.support.ActiveStatus;
import com.ecommerce.catalog.support.Slugs;
import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Quản trị sản phẩm và SKU (FR-CAT-005..011). Mọi thay đổi: audit + cập nhật chỉ mục + ProductChanged. */
@Service
@RequiredArgsConstructor
public class ProductAdminService {
    private static final int MAX_ATTRIBUTES = 10;

    private final ProductRepository products;
    private final SkuRepository skus;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ImageService images;
    private final ProductEvents productEvents;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<ProductDtos.ProductListItem> search(String q, ProductStatus status, UUID categoryId,
                                                            UUID brandId, Pageable pageable) {
        String keyword = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        return PageResponse.of(products.adminSearch(keyword, status, categoryId, brandId, pageable),
                ProductDtos.ProductListItem::from);
    }

    @Transactional(readOnly = true)
    public ProductDtos.ProductAdminResponse get(UUID id) {
        return toResponse(load(id));
    }

    @Transactional
    public ProductDtos.ProductAdminResponse create(ProductDtos.ProductCreateRequest req) {
        validateReferences(req.categoryId(), req.brandId());
        Instant now = clock.instant();
        Product product = new Product(req.name().trim(), resolveSlug(req.slug(), req.name(), null),
                req.description(), req.categoryId(), req.brandId(), now);
        products.save(product);
        if (req.skus() != null) {
            req.skus().forEach(s -> skus.save(newSku(product.getId(), s, now)));
        }
        productEvents.changed(product, "CREATED");
        audit.record("PRODUCT_CREATED", "PRODUCT", product.getId(), null, toResponse(product));
        return toResponse(product);
    }

    @Transactional
    public ProductDtos.ProductAdminResponse update(UUID id, ProductDtos.ProductUpdateRequest req) {
        Product product = load(id);
        validateReferences(req.categoryId(), req.brandId());
        var before = toResponse(product);
        product.update(req.name().trim(), resolveSlug(req.slug(), req.name(), product), req.description(),
                req.categoryId(), req.brandId(), clock.instant());
        productEvents.changed(product, "UPDATED");
        audit.record("PRODUCT_UPDATED", "PRODUCT", id, before, toResponse(product));
        return toResponse(product);
    }

    /**
     * FR-CAT-010: chỉ công bố được khi có ít nhất một SKU đang bán và danh mục đang hoạt động.
     */
    @Transactional
    public ProductDtos.ProductAdminResponse changeStatus(UUID id, ProductStatus status) {
        Product product = load(id);
        ProductStatus before = product.getStatus();
        if (status == ProductStatus.PUBLISHED) {
            boolean hasActiveSku = skus.findByProductIdOrderByCreatedAtAsc(id).stream()
                    .anyMatch(s -> s.getStatus() == SkuStatus.ACTIVE);
            if (!hasActiveSku) {
                throw new AppException(CatalogErrorCode.NOT_PUBLISHABLE, "Product needs at least one active SKU");
            }
            Category category = categories.findById(product.getCategoryId()).orElseThrow();
            if (category.getStatus() != ActiveStatus.ACTIVE) {
                throw new AppException(CatalogErrorCode.NOT_PUBLISHABLE, "Product category is inactive");
            }
        }
        product.changeStatus(status, clock.instant());
        productEvents.changed(product, status == ProductStatus.PUBLISHED ? "PUBLISHED" : "UNPUBLISHED");
        audit.record("PRODUCT_STATUS_CHANGED", "PRODUCT", id, Map.of("status", before), Map.of("status", status));
        return toResponse(product);
    }

    @Transactional
    public ProductDtos.SkuResponse addSku(UUID productId, ProductDtos.SkuCreateRequest req) {
        Product product = load(productId);
        Sku sku = skus.save(newSku(productId, req, clock.instant()));
        product.touch(clock.instant());
        productEvents.changed(product, "SKU_ADDED");
        audit.record("SKU_CREATED", "SKU", sku.getId(), null, ProductDtos.SkuResponse.from(sku));
        return ProductDtos.SkuResponse.from(sku);
    }

    @Transactional
    public ProductDtos.SkuResponse updateSku(UUID skuId, ProductDtos.SkuUpdateRequest req) {
        Sku sku = loadSku(skuId);
        var before = ProductDtos.SkuResponse.from(sku);
        sku.update(trimToNull(req.variantName()), normalizeAttributes(req.attributes()), req.maxPerOrder(),
                clock.instant());
        touchProduct(sku.getProductId(), "SKU_UPDATED");
        audit.record("SKU_UPDATED", "SKU", skuId, before, ProductDtos.SkuResponse.from(sku));
        return ProductDtos.SkuResponse.from(sku);
    }

    /** FR-CAT-011, BR-014: đổi giá ghi rõ ai, lúc nào, giá cũ -> giá mới, lý do. */
    @Transactional
    public ProductDtos.SkuResponse changePrice(UUID skuId, ProductDtos.PriceChangeRequest req) {
        Sku sku = loadSku(skuId);
        BigDecimal before = sku.getPrice();
        BigDecimal after = req.price().setScale(2, RoundingMode.HALF_UP);
        if (before.compareTo(after) != 0) {
            sku.changePrice(after, clock.instant());
            touchProduct(sku.getProductId(), "PRICE_CHANGED");
            audit.record("SKU_PRICE_CHANGED", "SKU", skuId, Map.of("price", before, "currency", sku.getCurrency()),
                    Map.of("price", after, "currency", sku.getCurrency(), "reason", req.reason()));
        }
        return ProductDtos.SkuResponse.from(sku);
    }

    @Transactional
    public ProductDtos.SkuResponse changeSkuStatus(UUID skuId, SkuStatus status) {
        Sku sku = loadSku(skuId);
        SkuStatus before = sku.getStatus();
        sku.changeStatus(status, clock.instant());
        touchProduct(sku.getProductId(), "SKU_STATUS_CHANGED");
        audit.record("SKU_STATUS_CHANGED", "SKU", skuId, Map.of("status", before), Map.of("status", status));
        return ProductDtos.SkuResponse.from(sku);
    }

    private void touchProduct(UUID productId, String changeType) {
        Product product = load(productId);
        product.touch(clock.instant());
        productEvents.changed(product, changeType);
    }

    private Sku newSku(UUID productId, ProductDtos.SkuCreateRequest req, Instant now) {
        String code = req.skuCode().trim().toUpperCase(Locale.ROOT);
        if (skus.existsBySkuCode(code)) {
            throw new AppException(CatalogErrorCode.SKU_CODE_ALREADY_USED, "SKU code " + code + " is already in use");
        }
        return new Sku(productId, code, trimToNull(req.variantName()), normalizeAttributes(req.attributes()),
                req.price().setScale(2, RoundingMode.HALF_UP), req.currency() == null ? "VND" : req.currency(),
                req.maxPerOrder() == null ? 10 : req.maxPerOrder(), now);
    }

    /** Chuẩn hóa thuộc tính biến thể: key chữ thường, bỏ khoảng trắng thừa, giới hạn số lượng/độ dài. */
    static Map<String, String> normalizeAttributes(Map<String, String> raw) {
        if (raw == null) {
            return new TreeMap<>();
        }
        if (raw.size() > MAX_ATTRIBUTES) {
            throw AppException.validation("attributes", "TOO_MANY_ATTRIBUTES");
        }
        Map<String, String> result = new HashMap<>();
        raw.forEach((k, v) -> {
            String key = k == null ? "" : k.trim().toLowerCase(Locale.ROOT);
            String value = v == null ? "" : v.trim();
            if (!key.matches("^[a-z0-9_]{1,50}$") || value.isEmpty() || value.length() > 100) {
                throw AppException.validation("attributes." + k, "INVALID_ATTRIBUTE");
            }
            result.put(key, value);
        });
        return new TreeMap<>(result);
    }

    private void validateReferences(UUID categoryId, UUID brandId) {
        if (!categories.existsById(categoryId)) {
            throw AppException.validation("categoryId", "NOT_FOUND");
        }
        if (brandId != null && !brands.existsById(brandId)) {
            throw AppException.validation("brandId", "NOT_FOUND");
        }
    }

    private String resolveSlug(String requested, String name, Product current) {
        if (requested != null && !requested.isBlank()) {
            String slug = requested.trim().toLowerCase(Locale.ROOT);
            if (!Slugs.isValid(slug)) {
                throw AppException.validation("slug", "INVALID_SLUG");
            }
            if ((current == null || !current.getSlug().equals(slug)) && products.existsBySlug(slug)) {
                throw new AppException(CatalogErrorCode.SLUG_ALREADY_USED);
            }
            return slug;
        }
        return current != null ? current.getSlug() : Slugs.unique(Slugs.slugify(name), products::existsBySlug);
    }

    private ProductDtos.ProductAdminResponse toResponse(Product p) {
        List<ProductDtos.SkuResponse> skuList = skus.findByProductIdOrderByCreatedAtAsc(p.getId()).stream()
                .map(ProductDtos.SkuResponse::from).toList();
        return new ProductDtos.ProductAdminResponse(p.getId(), p.getName(), p.getSlug(), p.getDescription(),
                p.getCategoryId(), p.getBrandId(), p.getStatus(), p.getVersion(), skuList, images.list(p.getId()),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private Product load(UUID id) {
        return products.findById(id).orElseThrow(() -> AppException.notFound("Product"));
    }

    private Sku loadSku(UUID id) {
        return skus.findById(id).orElseThrow(() -> AppException.notFound("SKU"));
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
