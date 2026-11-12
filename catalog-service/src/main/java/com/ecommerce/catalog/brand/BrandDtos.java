package com.ecommerce.catalog.brand;

import com.ecommerce.catalog.support.ActiveStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class BrandDtos {
    private BrandDtos() {
    }

    public record BrandRequest(@NotBlank @Size(max = 100) String name, @Size(max = 120) String slug) {
    }

    public record BrandResponse(UUID id, String name, String slug, ActiveStatus status) {
        public static BrandResponse from(Brand b) {
            return new BrandResponse(b.getId(), b.getName(), b.getSlug(), b.getStatus());
        }
    }
}
