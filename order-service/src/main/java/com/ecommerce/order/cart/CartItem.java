package com.ecommerce.order.cart;

import com.ecommerce.common.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends AssignedIdEntity {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;
    @Column(name = "sku_id", nullable = false)
    private UUID skuId;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "unit_price_at_add", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPriceAtAdd;
    @Column(name = "added_at", nullable = false)
    private Instant addedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    CartItem(Cart cart, UUID skuId, int quantity, BigDecimal unitPrice, Instant now) {
        this.id = UUID.randomUUID();
        this.cart = cart;
        this.skuId = skuId;
        this.quantity = quantity;
        this.unitPriceAtAdd = unitPrice;
        this.addedAt = now;
        this.updatedAt = now;
    }

    public void changeQuantity(int quantity, Instant now) {
        this.quantity = quantity;
        this.updatedAt = now;
    }

    /** Khách đã xem giá mới (vd: mở lại giỏ) => cập nhật mốc so sánh */
    public void acknowledgePrice(BigDecimal price) {
        this.unitPriceAtAdd = price;
    }
}
