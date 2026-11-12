package com.ecommerce.catalog.image;

import com.ecommerce.common.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "sku_id")
    private UUID skuId;
    @Column(name = "file_name", nullable = false)
    private String fileName;
    @Column(name = "original_name")
    private String originalName;
    @Column(name = "content_type", nullable = false)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(name = "is_primary", nullable = false)
    private boolean primary;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ProductImage(UUID productId, UUID skuId, String fileName, String originalName, String contentType,
                        long sizeBytes, int sortOrder, Instant now) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.skuId = skuId;
        this.fileName = fileName;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sortOrder = sortOrder;
        this.createdAt = now;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
