package com.ecommerce.catalog.internal;

import com.ecommerce.catalog.brand.Brand;
import com.ecommerce.catalog.brand.BrandRepository;
import com.ecommerce.catalog.category.Category;
import com.ecommerce.catalog.category.CategoryRepository;
import com.ecommerce.catalog.image.ImageService;
import com.ecommerce.catalog.product.Product;
import com.ecommerce.catalog.product.ProductDtos;
import com.ecommerce.catalog.product.ProductRepository;
import com.ecommerce.catalog.product.ProductStatus;
import com.ecommerce.catalog.sku.Sku;
import com.ecommerce.catalog.sku.SkuRepository;
import com.ecommerce.catalog.sku.SkuStatus;
import com.ecommerce.catalog.support.ActiveStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkuLookupService {

    private final SkuRepository skus;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ImageService images;

    @Transactional(readOnly = true)
    public List<SkuSnapshot> lookup(List<UUID> skuIds) {
        List<Sku> found = skus.findAllById(skuIds.stream().distinct().toList());
        Map<UUID, Product> productById = products.findAllById(found.stream().map(Sku::getProductId).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<UUID, Category> categoryById = categories.findAllById(productById.values().stream()
                .map(Product::getCategoryId).distinct().toList()).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<UUID, Brand> brandById = brands.findAllById(productById.values().stream()
                .map(Product::getBrandId).filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(Brand::getId, Function.identity()));

        return found.stream().map(sku -> {
            Product p = productById.get(sku.getProductId());
            Category c = categoryById.get(p.getCategoryId());
            Brand b = p.getBrandId() == null ? null : brandById.get(p.getBrandId());
            boolean sellable = sku.getStatus() == SkuStatus.ACTIVE && p.getStatus() == ProductStatus.PUBLISHED
                    && c != null && c.getStatus() == ActiveStatus.ACTIVE
                    && (b == null || b.getStatus() == ActiveStatus.ACTIVE);
            String image = images.list(p.getId()).stream()
                    .filter(i -> sku.getId().equals(i.skuId()) || i.primary())
                    .map(ProductDtos.ImageResponse::url).findFirst().orElse(null);
            return new SkuSnapshot(sku.getId(), sku.getSkuCode(), p.getId(), p.getName(), sku.getVariantName(),
                    sku.getAttributes(), sku.getPrice(), sku.getCurrency(), sellable, sku.getMaxPerOrder(), image);
        }).toList();
    }
}
