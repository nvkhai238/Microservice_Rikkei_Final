package com.ecommerce.catalog.brand;

import com.ecommerce.catalog.CatalogErrorCode;
import com.ecommerce.catalog.product.ProductRepository;
import com.ecommerce.catalog.support.ActiveStatus;
import com.ecommerce.catalog.support.Slugs;
import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** FR-CAT-003, FR-CAT-004 */
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brands;
    private final ProductRepository products;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<BrandDtos.BrandResponse> listActive() {
        return brands.findByStatusOrderByNameAsc(ActiveStatus.ACTIVE).stream().map(BrandDtos.BrandResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BrandDtos.BrandResponse> listAll() {
        return brands.findAllByOrderByNameAsc().stream().map(BrandDtos.BrandResponse::from).toList();
    }

    @Transactional
    public BrandDtos.BrandResponse create(BrandDtos.BrandRequest req) {
        Brand brand = new Brand(req.name().trim(), resolveSlug(req.slug(), req.name(), null), clock.instant());
        brands.save(brand);
        audit.record("BRAND_CREATED", "BRAND", brand.getId(), null, BrandDtos.BrandResponse.from(brand));
        return BrandDtos.BrandResponse.from(brand);
    }

    @Transactional
    public BrandDtos.BrandResponse update(UUID id, BrandDtos.BrandRequest req) {
        Brand brand = load(id);
        var before = BrandDtos.BrandResponse.from(brand);
        brand.update(req.name().trim(), resolveSlug(req.slug(), req.name(), brand), clock.instant());
        audit.record("BRAND_UPDATED", "BRAND", id, before, BrandDtos.BrandResponse.from(brand));
        return BrandDtos.BrandResponse.from(brand);
    }

    @Transactional
    public BrandDtos.BrandResponse changeStatus(UUID id, ActiveStatus status) {
        Brand brand = load(id);
        ActiveStatus before = brand.getStatus();
        brand.changeStatus(status, clock.instant());
        audit.record("BRAND_STATUS_CHANGED", "BRAND", id, Map.of("status", before), Map.of("status", status));
        return BrandDtos.BrandResponse.from(brand);
    }

    @Transactional
    public void delete(UUID id) {
        Brand brand = load(id);
        if (products.existsByBrandId(id)) {
            throw new AppException(CatalogErrorCode.IN_USE);
        }
        brands.delete(brand);
        audit.record("BRAND_DELETED", "BRAND", id, BrandDtos.BrandResponse.from(brand), null);
    }

    private String resolveSlug(String requested, String name, Brand current) {
        if (requested != null && !requested.isBlank()) {
            String slug = requested.trim().toLowerCase();
            if (!Slugs.isValid(slug)) {
                throw AppException.validation("slug", "INVALID_SLUG");
            }
            if ((current == null || !current.getSlug().equals(slug)) && brands.existsBySlug(slug)) {
                throw new AppException(CatalogErrorCode.SLUG_ALREADY_USED);
            }
            return slug;
        }
        return current != null ? current.getSlug() : Slugs.unique(Slugs.slugify(name), brands::existsBySlug);
    }

    private Brand load(UUID id) {
        return brands.findById(id).orElseThrow(() -> AppException.notFound("Brand"));
    }
}
