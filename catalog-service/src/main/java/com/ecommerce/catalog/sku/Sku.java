package com.ecommerce.catalog.sku;

import com.ecommerce.common.persistence.AssignedIdEntity;
import com.ecommerce.catalog.support.AttributesConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Biến thể có thể bán (FR-CAT-006, FR-CAT-007). Giá là BigDecimal, KHÔNG BAO GIỜ double (FR-CAT-008). */
@Entity
@Table(name = "skus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sku extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "sku_code", nullable = false)
    private String skuCode;
    @Column(name = "variant_name")
    private String variantName;
    @Convert(converter = AttributesConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private Map<String, String> attributes = new TreeMap<>();
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkuStatus status;
    @Column(name = "max_per_order", nullable = false)
    private int maxPerOrder;
    @Column(name = "available_quantity", nullable = false)
    private long availableQuantity;
    @Column(name = "stock_version", nullable = false)
    private long stockVersion;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public Sku(UUID productId, String skuCode, String variantName, Map<String, String> attributes, BigDecimal price,
               String currency, int maxPerOrder, Instant now) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.skuCode = skuCode;
        this.variantName = variantName;
        this.attributes = new TreeMap<>(attributes);
        this.price = price;
        this.currency = currency;
        this.maxPerOrder = maxPerOrder;
        this.status = SkuStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String variantName, Map<String, String> attributes, int maxPerOrder, Instant now) {
        this.variantName = variantName;
        this.attributes = new TreeMap<>(attributes);
        this.maxPerOrder = maxPerOrder;
        this.updatedAt = now;
    }

    public void changePrice(BigDecimal price, Instant now) {
        this.price = price;
        this.updatedAt = now;
    }

    public void changeStatus(SkuStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }
}
