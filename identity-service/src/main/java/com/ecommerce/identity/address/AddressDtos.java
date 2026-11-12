package com.ecommerce.identity.address;

import com.ecommerce.identity.user.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AddressDtos {
    private AddressDtos() {
    }

    /** FR-IAM-016: xác thực cấu trúc địa chỉ */
    public record AddressRequest(
            @NotBlank @Size(max = 100) String recipientName,
            @NotBlank @Pattern(regexp = ValidationPatterns.VN_PHONE) String phone,
            @NotBlank @Size(max = 200) String line1,
            @NotBlank @Size(max = 100) String ward,
            @NotBlank @Size(max = 100) String district,
            @NotBlank @Size(max = 100) String province,
            @Pattern(regexp = "^\\d{5,6}$") String postalCode,
            Boolean makeDefault) {
    }

    public record AddressResponse(UUID id, String recipientName, String phone, String line1, String ward,
                                  String district, String province, String postalCode, boolean isDefault) {
        static AddressResponse from(Address a) {
            return new AddressResponse(a.getId(), a.getRecipientName(), a.getPhone(), a.getLine1(), a.getWard(),
                    a.getDistrict(), a.getProvince(), a.getPostalCode(), a.isDefaultAddress());
        }
    }
}
