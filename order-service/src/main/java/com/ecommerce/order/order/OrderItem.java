package com.ecommerce.order.order;

import com.ecommerce.common.persistence.AssignedIdEntity;
import com.ecommerce.order.support.JsonMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Snapshot dòng hàng tại thời điểm mua (FR-ORD-004). Bất biến: catalog đổi giá sau đó không ảnh hưởng (BR-002). */
@Entity
@Immutable
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "sku_id", nullable = false)
    private UUID skuId;
    @Column(name = "sku_code", nullable = false)
    private String skuCode;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "product_name", nullable = false)
    private String productName;
    @Column(name = "variant_name")
    private String variantName;
    @Convert(converter = JsonMapConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private Map<String, String> attributes;
    @Column(name = "image_url")
    private String imageUrl;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discount;
    @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    public OrderItem(UUID skuId, String skuCode, UUID productId, String productName, String variantName,
                     Map<String, String> attributes, String imageUrl, BigDecimal unitPrice, int quantity) {
        this.id = UUID.randomUUID();
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.productId = productId;
        this.productName = productName;
        this.variantName = variantName;
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        this.imageUrl = imageUrl;
        this.unitPrice = Money.normalize(unitPrice);
        this.quantity = quantity;
        this.discount = Money.ZERO;
        this.lineTotal = Money.lineTotal(this.unitPrice, quantity, this.discount);
    }
}
