package com.ecommerce.order.cart;

import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartDtos.CartView> view() {
        return ApiResponse.success(cartService.view(CurrentUser.id()));
    }

    @PostMapping("/items")
    public ApiResponse<CartDtos.CartView> add(@Valid @RequestBody CartDtos.AddItemRequest request) {
        return ApiResponse.success(cartService.addItem(CurrentUser.id(), request));
    }

    @PutMapping("/items/{skuId}")
    public ApiResponse<CartDtos.CartView> update(@PathVariable UUID skuId,
                                                 @Valid @RequestBody CartDtos.UpdateItemRequest request) {
        return ApiResponse.success(cartService.updateItem(CurrentUser.id(), skuId, request.quantity()));
    }

    @DeleteMapping("/items/{skuId}")
    public ApiResponse<CartDtos.CartView> remove(@PathVariable UUID skuId) {
        return ApiResponse.success(cartService.removeItem(CurrentUser.id(), skuId));
    }

    @DeleteMapping
    public ApiResponse<Void> clear() {
        cartService.clear(CurrentUser.id());
        return ApiResponse.success(null, "Cart cleared");
    }
}
