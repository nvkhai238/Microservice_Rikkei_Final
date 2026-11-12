package com.ecommerce.catalog.search;

import com.ecommerce.catalog.image.ImageService;
import com.ecommerce.catalog.product.ProductDtos;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tìm kiếm sản phẩm ĐANG BÁN (UC-02):
 * <ul>
 *   <li>Chỉ sản phẩm PUBLISHED, có SKU ACTIVE, danh mục/thương hiệu ACTIVE (FR-CAT-010, FR-CAT-013).</li>
 *   <li>Full-text trên search_vector, bỏ dấu tiếng Việt, khớp tiền tố ("iph" ra "iphone") (FR-CAT-014).</li>
 *   <li>Lọc danh mục (gồm cả danh mục con), thương hiệu, khoảng giá, còn hàng, thuộc tính biến thể (FR-CAT-015).</li>
 *   <li>Sắp xếp + luôn kèm p.id làm tiêu chí phụ => phân trang ổn định, không lặp/mất dòng (FR-CAT-013, 016).</li>
 * </ul>
 * SQL được ghép từ các mảnh CỐ ĐỊNH, giá trị người dùng chỉ đi qua tham số :name => không SQL injection (DR-010).
 */
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private static final Set<String> SORTS = Set.of("relevance", "price_asc", "price_desc", "newest", "updated");

    private final NamedParameterJdbcTemplate jdbc;
    private final ImageService imageService;
    private final JsonMapper jsonMapper;

    @Transactional(readOnly = true)
    public PageResponse<ProductDtos.ProductSummary> search(ProductSearchCriteria c) {
        String sort = c.sort() == null ? null : c.sort().toLowerCase(Locale.ROOT);
        if (sort != null && !SORTS.contains(sort)) {
            throw AppException.validation("sort", "UNSUPPORTED_SORT");
        }
        if (c.minPrice() != null && c.maxPrice() != null && c.minPrice().compareTo(c.maxPrice()) > 0) {
            throw AppException.validation("minPrice", "GREATER_THAN_MAX_PRICE");
        }
        String tsQuery = toPrefixTsQuery(c.q());
        if (sort == null) {
            sort = tsQuery != null ? "relevance" : "newest";
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> where = new ArrayList<>();
        where.add("p.status = 'PUBLISHED'");
        where.add("(b.id IS NULL OR b.status = 'ACTIVE')");
        String cte = "";
        if (tsQuery != null) {
            where.add("p.search_vector @@ to_tsquery('simple', unaccent(:tsq))");
            params.addValue("tsq", tsQuery);
        }
        if (c.categoryId() != null) {
            // Lọc theo danh mục cha thì ra cả sản phẩm của danh mục con/cháu
            cte = """
                    WITH RECURSIVE cat_tree AS (
                        SELECT id FROM categories WHERE id = :categoryId AND status = 'ACTIVE'
                        UNION ALL
                        SELECT ch.id FROM categories ch JOIN cat_tree t ON ch.parent_id = t.id WHERE ch.status = 'ACTIVE'
                    )
                    """;
            where.add("p.category_id IN (SELECT id FROM cat_tree)");
            params.addValue("categoryId", c.categoryId());
        }
        if (c.brandId() != null) {
            where.add("p.brand_id = :brandId");
            params.addValue("brandId", c.brandId());
        }
        if (c.minPrice() != null) {
            where.add("s.price >= :minPrice");
            params.addValue("minPrice", c.minPrice());
        }
        if (c.maxPrice() != null) {
            where.add("s.price <= :maxPrice");
            params.addValue("maxPrice", c.maxPrice());
        }
        if (Boolean.TRUE.equals(c.inStock())) {
            where.add("s.available_quantity > 0");
        }
        if (c.attributes() != null && !c.attributes().isEmpty()) {
            // jsonb @> : SKU có chứa đủ các cặp thuộc tính được lọc, vd {"color":"Đỏ","size":"M"}
            where.add("CAST(s.attributes AS jsonb) @> CAST(:attrs AS jsonb)");
            params.addValue("attrs", jsonMapper.writeValueAsString(c.attributes()));
        }

        String from = """
                FROM products p
                JOIN categories c ON c.id = p.category_id AND c.status = 'ACTIVE'
                LEFT JOIN brands b ON b.id = p.brand_id
                JOIN skus s ON s.product_id = p.id AND s.status = 'ACTIVE'
                WHERE %s
                GROUP BY p.id, b.name, c.name
                """.formatted(String.join(" AND ", where));

        Long total = jdbc.queryForObject(cte + "SELECT count(*) FROM (SELECT p.id " + from + ") t", params, Long.class);

        String rank = tsQuery != null ? "ts_rank(p.search_vector, to_tsquery('simple', unaccent(:tsq)))" : "0";
        String orderBy = switch (sort) {
            case "relevance" -> "rank DESC, p.updated_at DESC, p.id";
            case "price_asc" -> "min_price ASC, p.id";
            case "price_desc" -> "min_price DESC, p.id";
            case "updated" -> "p.updated_at DESC, p.id";
            default -> "p.created_at DESC, p.id";
        };
        params.addValue("limit", c.size()).addValue("offset", (long) c.page() * c.size());
        String sql = cte + """
                SELECT p.id, p.name, p.slug, b.name AS brand_name, c.name AS category_name,
                       MIN(s.price) AS min_price, MAX(s.price) AS max_price, MIN(s.currency) AS currency,
                       BOOL_OR(s.available_quantity > 0) AS in_stock,
                       (SELECT i.file_name FROM product_images i WHERE i.product_id = p.id
                        ORDER BY i.is_primary DESC, i.sort_order LIMIT 1) AS image,
                       %s AS rank, p.created_at, p.updated_at
                """.formatted(rank) + from + " ORDER BY " + orderBy + " LIMIT :limit OFFSET :offset";

        List<ProductDtos.ProductSummary> items = jdbc.query(sql, params, (rs, i) -> new ProductDtos.ProductSummary(
                rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"),
                rs.getString("brand_name"), rs.getString("category_name"), rs.getBigDecimal("min_price"),
                rs.getBigDecimal("max_price"), rs.getString("currency"), rs.getBoolean("in_stock"),
                imageService.urlOf(rs.getString("image")), toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))));
        return PageResponse.of(items, c.page(), c.size(), total == null ? 0 : total);
    }

    /**
     * "Áo thun đỏ" -> "áo:* & thun:* & đỏ:*". Chỉ giữ chữ/số => người dùng không chèn được toán tử tsquery.
     */
    static String toPrefixTsQuery(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        // Tách ở mọi ký tự không phải chữ/số: "CM-TEE" -> "cm", "tee" (khớp cách Postgres tách mã SKU có gạch)
        String terms = java.util.Arrays.stream(q.trim().split("[^\\p{L}\\p{N}]+"))
                .filter(w -> !w.isEmpty())
                .limit(10)
                .map(w -> w.toLowerCase(Locale.ROOT) + ":*")
                .collect(Collectors.joining(" & "));
        return terms.isEmpty() ? null : terms;
    }

    private static java.time.Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
