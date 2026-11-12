package com.ecommerce.catalog.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsBySlug(String slug);

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByOrderBySortOrderAscNameAsc();

    boolean existsByParentId(UUID parentId);

    /**
     * Lấy toàn bộ tổ tiên của một danh mục bằng CTE đệ quy (đi ngược lên gốc).
     * Dùng để chặn chu trình: không được chọn làm cha một danh mục đang là CON/CHÁU của chính nó.
     */
    @Query(value = """
            WITH RECURSIVE ancestors AS (
                SELECT id, parent_id, 1 AS depth FROM categories WHERE id = :id
                UNION ALL
                SELECT c.id, c.parent_id, a.depth + 1 FROM categories c
                JOIN ancestors a ON c.id = a.parent_id
                WHERE a.depth < 100
            )
            SELECT CAST(id AS VARCHAR) FROM ancestors
            """, nativeQuery = true)
    List<String> findAncestorIdsIncludingSelf(@Param("id") UUID id);
}
