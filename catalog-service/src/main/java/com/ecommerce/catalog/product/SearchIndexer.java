package com.ecommerce.catalog.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Tính lại cột search_vector (chỉ mục full-text) của sản phẩm (FR-CAT-012, FR-CAT-014).
 * Chạy TRONG transaction thay đổi sản phẩm => chỉ mục không bao giờ lệch với dữ liệu gốc.
 * Trọng số: A (tên, mã SKU) > B (thương hiệu, danh mục) > C (mô tả) => ảnh hưởng điểm liên quan.
 * Bảng products vẫn là nguồn sự thật; chỉ mục tái lập được bất cứ lúc nào (FR-CAT-018, DR-013).
 */
@Slf4j
@Component
public class SearchIndexer {

    private static final String REFRESH_SQL = """
            UPDATE products p SET search_vector =
                setweight(to_tsvector('simple', unaccent(coalesce(p.name, ''))), 'A') ||
                setweight(to_tsvector('simple', unaccent(coalesce((
                    SELECT string_agg(s.sku_code || ' ' || replace(s.sku_code, '-', ' ') || ' ' ||
                                      coalesce(s.variant_name, ''), ' ')
                    FROM skus s WHERE s.product_id = p.id), ''))), 'A') ||
                setweight(to_tsvector('simple', unaccent(coalesce((
                    SELECT b.name FROM brands b WHERE b.id = p.brand_id), ''))), 'B') ||
                setweight(to_tsvector('simple', unaccent(coalesce((
                    SELECT c.name FROM categories c WHERE c.id = p.category_id), ''))), 'B') ||
                setweight(to_tsvector('simple', unaccent(coalesce(p.description, ''))), 'C')
            """;

    private final JdbcTemplate jdbc;

    public SearchIndexer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void refresh(UUID productId) {
        jdbc.update(REFRESH_SQL + " WHERE p.id = ?", productId);
    }

    /** Tái lập chỉ mục cho toàn bộ sản phẩm (sau khi đổi công thức, khôi phục dữ liệu...) */
    @Transactional
    public int rebuildAll() {
        int updated = jdbc.update(REFRESH_SQL);
        log.info("Search index rebuilt for {} product(s)", updated);
        return updated;
    }

    /** Đổi tên brand/danh mục thì các sản phẩm liên quan cũng phải tính lại */
    @Transactional(propagation = Propagation.MANDATORY)
    public void refreshByCategoryOrBrand(UUID categoryId, UUID brandId) {
        jdbc.update(REFRESH_SQL + " WHERE p.category_id = ? OR p.brand_id = ?", categoryId, brandId);
    }
}
