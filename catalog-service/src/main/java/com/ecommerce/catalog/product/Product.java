package com.ecommerce.catalog.product;

import com.ecommerce.common.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Product chỉ là thực thể nhóm/mô tả; đơn vị bán và quản lý tồn là SKU (BR-001). */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String slug;
    @Column(columnDefinition = "text")
    private String description;
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;
    @Column(name = "brand_id")
    private UUID brandId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public Product(String name, String slug, String description, UUID categoryId, UUID brandId, Instant now) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.status = ProductStatus.DRAFT;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String slug, String description, UUID categoryId, UUID brandId, Instant now) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.updatedAt = now;
    }

    public void changeStatus(ProductStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    /** SKU/ảnh thay đổi cũng coi là sản phẩm thay đổi => tăng version, cập nhật updatedAt */
    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
