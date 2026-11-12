package com.ecommerce.catalog.category;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Danh mục phân cấp (FR-CAT-001, FR-CAT-002, FR-CAT-004). */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final AuditService audit;
    private final Clock clock;

    /** Cây danh mục ACTIVE. Con của danh mục ngừng hoạt động cũng bị ẩn theo. */
    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryNode> activeTree() {
        Map<UUID, CategoryDtos.CategoryNode> nodes = new LinkedHashMap<>();
        List<Category> all = categories.findAllByOrderBySortOrderAscNameAsc().stream()
                .filter(c -> c.getStatus() == ActiveStatus.ACTIVE).toList();
        all.forEach(c -> nodes.put(c.getId(), new CategoryDtos.CategoryNode(c)));
        List<CategoryDtos.CategoryNode> roots = new ArrayList<>();
        for (Category c : all) {
            CategoryDtos.CategoryNode node = nodes.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(node);
            } else if (nodes.containsKey(c.getParentId())) {
                nodes.get(c.getParentId()).children().add(node);
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> listAll() {
        return categories.findAllByOrderBySortOrderAscNameAsc().stream().map(CategoryDtos.CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDtos.CategoryResponse get(UUID id) {
        return CategoryDtos.CategoryResponse.from(load(id));
    }

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CategoryRequest req) {
        if (req.parentId() != null) {
            load(req.parentId());
        }
        String slug = resolveSlug(req.slug(), req.name(), null);
        Category category = new Category(req.name().trim(), slug, req.parentId(),
                req.sortOrder() == null ? 0 : req.sortOrder(), clock.instant());
        categories.save(category);
        audit.record("CATEGORY_CREATED", "CATEGORY", category.getId(), null, CategoryDtos.CategoryResponse.from(category));
        return CategoryDtos.CategoryResponse.from(category);
    }

    @Transactional
    public CategoryDtos.CategoryResponse update(UUID id, CategoryDtos.CategoryRequest req) {
        Category category = load(id);
        var before = CategoryDtos.CategoryResponse.from(category);
        if (req.parentId() != null) {
            load(req.parentId());
            // FR-CAT-002: chặn chu trình. Nếu "id" nằm trong chuỗi tổ tiên của parent mới
            // (kể cả chính parent) thì đặt parent đó sẽ tạo vòng A -> B -> ... -> A.
            if (categories.findAncestorIdsIncludingSelf(req.parentId()).contains(id.toString())) {
                throw new AppException(CatalogErrorCode.CATEGORY_CYCLE);
            }
        }
        String slug = resolveSlug(req.slug(), req.name(), category);
        category.update(req.name().trim(), slug, req.parentId(),
                req.sortOrder() == null ? category.getSortOrder() : req.sortOrder(), clock.instant());
        audit.record("CATEGORY_UPDATED", "CATEGORY", id, before, CategoryDtos.CategoryResponse.from(category));
        return CategoryDtos.CategoryResponse.from(category);
    }

    @Transactional
    public CategoryDtos.CategoryResponse changeStatus(UUID id, ActiveStatus status) {
        Category category = load(id);
        ActiveStatus before = category.getStatus();
        category.changeStatus(status, clock.instant());
        audit.record("CATEGORY_STATUS_CHANGED", "CATEGORY", id, Map.of("status", before), Map.of("status", status));
        return CategoryDtos.CategoryResponse.from(category);
    }

    /** FR-CAT-004: đang được tham chiếu thì KHÔNG xóa cứng, yêu cầu chuyển sang ngừng hoạt động. */
    @Transactional
    public void delete(UUID id) {
        Category category = load(id);
        if (products.existsByCategoryId(id) || categories.existsByParentId(id)) {
            throw new AppException(CatalogErrorCode.IN_USE);
        }
        categories.delete(category);
        audit.record("CATEGORY_DELETED", "CATEGORY", id, CategoryDtos.CategoryResponse.from(category), null);
    }

    private String resolveSlug(String requested, String name, Category current) {
        if (requested != null && !requested.isBlank()) {
            String slug = requested.trim().toLowerCase();
            if (!Slugs.isValid(slug)) {
                throw AppException.validation("slug", "INVALID_SLUG");
            }
            if ((current == null || !current.getSlug().equals(slug)) && categories.existsBySlug(slug)) {
                throw new AppException(CatalogErrorCode.SLUG_ALREADY_USED);
            }
            return slug;
        }
        if (current != null) {
            return current.getSlug();
        }
        return Slugs.unique(Slugs.slugify(name), categories::existsBySlug);
    }

    private Category load(UUID id) {
        return categories.findById(id).orElseThrow(() -> AppException.notFound("Category"));
    }
}
