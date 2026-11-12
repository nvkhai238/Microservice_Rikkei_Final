package com.ecommerce.order.cart;

import com.ecommerce.common.persistence.AssignedIdEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Một khách hàng có đúng một giỏ, dùng chung Web/Mobile (FR-CART-001). Thêm vào giỏ KHÔNG giữ kho (BR-003). */
@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("addedAt ASC")
    private List<CartItem> items = new ArrayList<>();
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public Cart(UUID customerId, Instant now) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Optional<CartItem> find(UUID skuId) {
        return items.stream().filter(i -> i.getSkuId().equals(skuId)).findFirst();
    }

    public CartItem add(UUID skuId, int quantity, BigDecimal unitPrice, Instant now) {
        CartItem item = new CartItem(this, skuId, quantity, unitPrice, now);
        items.add(item);
        updatedAt = now;
        return item;
    }

    public boolean remove(UUID skuId, Instant now) {
        updatedAt = now;
        return items.removeIf(i -> i.getSkuId().equals(skuId));
    }

    public void clear(Instant now) {
        items.clear();
        updatedAt = now;
    }

    public void touch(Instant now) {
        updatedAt = now;
    }
}
