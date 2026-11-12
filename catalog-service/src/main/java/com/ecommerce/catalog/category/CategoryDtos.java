package com.ecommerce.catalog.category;

import com.ecommerce.catalog.support.ActiveStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CategoryDtos {
    private CategoryDtos() {
    }

    public record CategoryRequest(@NotBlank @Size(max = 100) String name, @Size(max = 120) String slug,
                                  UUID parentId, @Min(0) @Max(10000) Integer sortOrder) {
    }

    public record StatusRequest(@NotNull ActiveStatus status) {
    }

    public record CategoryResponse(UUID id, String name, String slug, UUID parentId, ActiveStatus status,
                                   int sortOrder) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getParentId(), c.getStatus(),
                    c.getSortOrder());
        }
    }

    /** Cây danh mục cho trang chủ / menu */
    public record CategoryNode(UUID id, String name, String slug, List<CategoryNode> children) {
        public CategoryNode(Category c) {
            this(c.getId(), c.getName(), c.getSlug(), new ArrayList<>());
        }
    }
}
