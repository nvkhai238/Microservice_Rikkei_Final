package com.ecommerce.identity.address;

import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * order-service cũng gọi {@code GET /api/v1/users/me/addresses/{id}} (kèm token của khách) để lấy
 * snapshot địa chỉ khi tạo đơn => kiểm tra quyền sở hữu địa chỉ dùng chung một chỗ (FR-ORD-003).
 */
@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ApiResponse<List<AddressDtos.AddressResponse>> list() {
        return ApiResponse.success(addressService.list(CurrentUser.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<AddressDtos.AddressResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(addressService.get(CurrentUser.id(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddressDtos.AddressResponse> create(@Valid @RequestBody AddressDtos.AddressRequest request) {
        return ApiResponse.success(addressService.create(CurrentUser.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressDtos.AddressResponse> update(@PathVariable UUID id,
                                                           @Valid @RequestBody AddressDtos.AddressRequest request) {
        return ApiResponse.success(addressService.update(CurrentUser.id(), id, request));
    }

    @PutMapping("/{id}/default")
    public ApiResponse<AddressDtos.AddressResponse> makeDefault(@PathVariable UUID id) {
        return ApiResponse.success(addressService.makeDefault(CurrentUser.id(), id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        addressService.delete(CurrentUser.id(), id);
        return ApiResponse.success(null, "Address deleted");
    }
}
