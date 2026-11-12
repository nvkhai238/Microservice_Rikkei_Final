package com.ecommerce.order.cart;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.FieldErrorDetail;
import com.ecommerce.order.OrderErrorCode;
import com.ecommerce.order.client.CatalogClient;
import com.ecommerce.order.config.OrderProperties;
import com.ecommerce.order.inventory.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Giỏ hàng (FR-CART-001..008). Mỗi lần xem giỏ đều đánh giá lại giá / trạng thái bán / tồn kho hiện tại.
 */
@Slf4j
@Service
public class CartService {

    private final CartRepository carts;
    private final CatalogClient catalog;
    private final InventoryService inventory;
    private final OrderProperties.Cart limits;
    private final String currency;
    private final Clock clock;

    public CartService(CartRepository carts, CatalogClient catalog, InventoryService inventory,
                       OrderProperties props, Clock clock) {
        this.carts = carts;
        this.catalog = catalog;
        this.inventory = inventory;
        this.limits = props.cart();
        this.currency = props.order().currency();
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CartDtos.CartView view(UUID customerId) {
        return carts.findByCustomerId(customerId).map(this::toView)
                .orElseGet(() -> new CartDtos.CartView(null, List.of(), BigDecimal.ZERO, currency, false, List.of()));
    }

    @Transactional
    public CartDtos.CartView addItem(UUID customerId, CartDtos.AddItemRequest req) {
        // Gọi catalog TRƯỚC khi đụng DB: SKU phải tồn tại và đang bán (FR-CART-002)
        CatalogClient.SkuSnapshot sku = catalog.lookup(List.of(req.skuId())).get(req.skuId());
        if (sku == null || !sku.sellable()) {
            throw new AppException(OrderErrorCode.SKU_NOT_AVAILABLE, "SKU is not available for sale",
                    List.of(new FieldErrorDetail("skuId", "NOT_SELLABLE")));
        }
        Cart cart = loadOrCreate(customerId);
        Instant now = clock.instant();
        CartItem existing = cart.find(req.skuId()).orElse(null);
        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + req.quantity();
        checkQuantity(newQuantity, sku.maxPerOrder(), "quantity");
        if (existing == null) {
            if (cart.getItems().size() >= limits.maxLines()) {
                throw AppException.businessRule("CART_LINE_LIMIT", "Cart can hold at most " + limits.maxLines() + " lines");
            }
            cart.add(req.skuId(), newQuantity, sku.price(), now);
        } else {
            existing.changeQuantity(newQuantity, now);
            cart.touch(now);
        }
        carts.saveAndFlush(cart);
        return toView(cart);
    }

    @Transactional
    public CartDtos.CartView updateItem(UUID customerId, UUID skuId, int quantity) {
        Cart cart = carts.findByCustomerId(customerId).orElseThrow(() -> AppException.notFound("Cart item"));
        CartItem item = cart.find(skuId).orElseThrow(() -> AppException.notFound("Cart item"));
        CatalogClient.SkuSnapshot sku = catalog.lookup(List.of(skuId)).get(skuId);
        checkQuantity(quantity, sku == null ? limits.maxQuantityPerLine() : sku.maxPerOrder(), "quantity");
        item.changeQuantity(quantity, clock.instant());
        cart.touch(clock.instant());
        return toView(cart);
    }

    @Transactional
    public CartDtos.CartView removeItem(UUID customerId, UUID skuId) {
        Cart cart = carts.findByCustomerId(customerId).orElseThrow(() -> AppException.notFound("Cart item"));
        if (!cart.remove(skuId, clock.instant())) {
            throw AppException.notFound("Cart item");
        }
        return toView(cart);
    }

    @Transactional
    public void clear(UUID customerId) {
        carts.findByCustomerId(customerId).ifPresent(c -> c.clear(clock.instant()));
    }

    /** FR-CART-004: giới hạn theo SKU và theo cấu hình hệ thống, lấy cái nhỏ hơn. */
    private void checkQuantity(int quantity, int skuLimit, String field) {
        int limit = Math.min(skuLimit, limits.maxQuantityPerLine());
        if (quantity > limit) {
            throw new AppException(OrderErrorCode.QUANTITY_LIMIT_EXCEEDED, "Maximum " + limit + " per order for this item",
                    List.of(new FieldErrorDetail(field, "MAX_" + limit)));
        }
    }

    private Cart loadOrCreate(UUID customerId) {
        return carts.findByCustomerId(customerId).orElseGet(() -> {
            carts.createIfAbsent(UUID.randomUUID(), customerId);
            return carts.findByCustomerId(customerId).orElseThrow();
        });
    }

    /** Ghép dữ liệu giỏ + catalog + tồn kho thành view, kèm cảnh báo cho từng dòng. */
    private CartDtos.CartView toView(Cart cart) {
        List<UUID> skuIds = cart.getItems().stream().map(CartItem::getSkuId).toList();
        Map<UUID, CatalogClient.SkuSnapshot> skus;
        List<String> cartWarnings = new ArrayList<>();
        try {
            skus = catalog.lookup(skuIds);
        } catch (AppException catalogDown) {
            // NFR-AVL-007: catalog sập thì vẫn xem được giỏ (thiếu thông tin), chỉ không checkout được
            skus = Map.of();
            cartWarnings.add("CATALOG_UNAVAILABLE");
        }
        Map<UUID, Long> available = inventory.availability(skuIds);
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean ready = !cart.getItems().isEmpty() && cartWarnings.isEmpty();
        List<CartDtos.CartLine> lines = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            CatalogClient.SkuSnapshot sku = skus.get(item.getSkuId());
            List<String> warnings = new ArrayList<>();
            long stock = available.getOrDefault(item.getSkuId(), 0L);
            BigDecimal price = sku == null ? item.getUnitPriceAtAdd() : sku.price();
            if (sku == null || !sku.sellable()) {
                warnings.add("NOT_SELLABLE");
            }
            if (sku != null && sku.price().compareTo(item.getUnitPriceAtAdd()) != 0) {
                warnings.add("PRICE_CHANGED");
            }
            if (stock < item.getQuantity()) {
                warnings.add("INSUFFICIENT_STOCK");
            }
            if (sku != null && item.getQuantity() > Math.min(sku.maxPerOrder(), limits.maxQuantityPerLine())) {
                warnings.add("QUANTITY_LIMIT_EXCEEDED");
            }
            if (warnings.contains("NOT_SELLABLE") || warnings.contains("INSUFFICIENT_STOCK")
                    || warnings.contains("QUANTITY_LIMIT_EXCEEDED")) {
                ready = false;
            }
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            lines.add(new CartDtos.CartLine(item.getSkuId(), sku == null ? null : sku.skuCode(),
                    sku == null ? null : sku.productName(), sku == null ? null : sku.variantName(),
                    sku == null ? Map.of() : sku.attributes(), sku == null ? null : sku.imageUrl(),
                    item.getQuantity(), price, item.getUnitPriceAtAdd(), lineTotal, sku != null && sku.sellable(),
                    stock, sku == null ? 0 : sku.maxPerOrder(), warnings));
        }
        return new CartDtos.CartView(cart.getId(), lines, subtotal, currency, ready, cartWarnings);
    }
}
