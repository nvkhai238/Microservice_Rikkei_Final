package com.ecommerce.catalog.product;

import com.ecommerce.catalog.brand.Brand;
import com.ecommerce.catalog.brand.BrandRepository;
import com.ecommerce.catalog.category.Category;
import com.ecommerce.catalog.category.CategoryRepository;
import com.ecommerce.catalog.image.ImageService;
import com.ecommerce.catalog.sku.Sku;
import com.ecommerce.catalog.sku.SkuRepository;
import com.ecommerce.catalog.sku.SkuStatus;
import com.ecommerce.catalog.support.ActiveStatus;
import com.ecommerce.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Trang chi tiết sản phẩm cho khách (FR-CAT-017). Sản phẩm chưa công bố => 404 như không tồn tại. */
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository products;
    private final SkuRepository skus;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ImageService images;

    @Transactional(readOnly = true)
    public ProductDtos.ProductDetail detail(String idOrSlug) {
        Product product = find(idOrSlug).filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                .orElseThrow(() -> AppException.notFound("Product"));
        Category category = categories.findById(product.getCategoryId())
                .filter(c -> c.getStatus() == ActiveStatus.ACTIVE)
                .orElseThrow(() -> AppException.notFound("Product"));
        Brand brand = product.getBrandId() == null ? null : brands.findById(product.getBrandId()).orElse(null);
        if (brand != null && brand.getStatus() != ActiveStatus.ACTIVE) {
            throw AppException.notFound("Product");
        }
        List<Sku> active = skus.findByProductIdOrderByCreatedAtAsc(product.getId()).stream()
                .filter(s -> s.getStatus() == SkuStatus.ACTIVE).toList();
        if (active.isEmpty()) {
            throw AppException.notFound("Product");
        }
        List<ProductDtos.PublicSku> skuList = active.stream()
                .map(s -> new ProductDtos.PublicSku(s.getId(), s.getSkuCode(), s.getVariantName(), s.getAttributes(),
                        s.getPrice(), s.getCurrency(), s.getMaxPerOrder(), s.getAvailableQuantity() > 0))
                .toList();
        BigDecimal min = active.stream().map(Sku::getPrice).min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal max = active.stream().map(Sku::getPrice).max(Comparator.naturalOrder()).orElseThrow();
        return new ProductDtos.ProductDetail(product.getId(), product.getName(), product.getSlug(),
                product.getDescription(),
                new ProductDtos.Ref(category.getId(), category.getName(), category.getSlug()),
                brand == null ? null : new ProductDtos.Ref(brand.getId(), brand.getName(), brand.getSlug()),
                min, max, active.get(0).getCurrency(), skuList.stream().anyMatch(ProductDtos.PublicSku::inStock),
                skuList, images.list(product.getId()), product.getUpdatedAt());
    }

    private java.util.Optional<Product> find(String idOrSlug) {
        try {
            return products.findById(UUID.fromString(idOrSlug));
        } catch (IllegalArgumentException notUuid) {
            return products.findBySlug(idOrSlug.toLowerCase());
        }
    }
}
