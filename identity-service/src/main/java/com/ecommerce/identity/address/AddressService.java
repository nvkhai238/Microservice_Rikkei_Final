package com.ecommerce.identity.address;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.identity.user.Normalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sổ địa chỉ (FR-IAM-015, FR-IAM-016, BR-013).
 * Chống IDOR: userId LUÔN lấy từ token; địa chỉ không thuộc user => 404 (không nói "của người khác").
 */
@Service
@RequiredArgsConstructor
public class AddressService {
    static final int MAX_ADDRESSES = 20;

    private final AddressRepository addresses;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AddressDtos.AddressResponse> list(UUID userId) {
        return addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream()
                .map(AddressDtos.AddressResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AddressDtos.AddressResponse get(UUID userId, UUID addressId) {
        return AddressDtos.AddressResponse.from(load(userId, addressId));
    }

    @Transactional
    public AddressDtos.AddressResponse create(UUID userId, AddressDtos.AddressRequest req) {
        long count = addresses.countByUserId(userId);
        if (count >= MAX_ADDRESSES) {
            throw AppException.businessRule("ADDRESS_LIMIT_REACHED", "Maximum " + MAX_ADDRESSES + " addresses");
        }
        Instant now = clock.instant();
        Address address = new Address(userId, now);
        address.apply(req, Normalizer.phone(req.phone()), now);
        // Địa chỉ đầu tiên tự động là mặc định
        boolean makeDefault = count == 0 || Boolean.TRUE.equals(req.makeDefault());
        if (makeDefault) {
            addresses.clearDefault(userId);
        }
        address.setDefault(makeDefault, now);
        return AddressDtos.AddressResponse.from(addresses.save(address));
    }

    @Transactional
    public AddressDtos.AddressResponse update(UUID userId, UUID addressId, AddressDtos.AddressRequest req) {
        Address address = load(userId, addressId);
        Instant now = clock.instant();
        address.apply(req, Normalizer.phone(req.phone()), now);
        if (Boolean.TRUE.equals(req.makeDefault()) && !address.isDefaultAddress()) {
            addresses.clearDefault(userId);
            address.setDefault(true, now);
        }
        return AddressDtos.AddressResponse.from(address);
    }

    @Transactional
    public AddressDtos.AddressResponse makeDefault(UUID userId, UUID addressId) {
        Address address = load(userId, addressId);
        if (!address.isDefaultAddress()) {
            // Bỏ mặc định cũ TRƯỚC (flush ngay) rồi mới đặt mặc định mới => không vi phạm unique index
            addresses.clearDefault(userId);
            address.setDefault(true, clock.instant());
        }
        return AddressDtos.AddressResponse.from(address);
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        Address address = load(userId, addressId);
        addresses.delete(address);
        addresses.flush();
        if (address.isDefaultAddress()) {
            // Xóa địa chỉ mặc định thì chọn địa chỉ mới nhất còn lại làm mặc định
            addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream().findFirst()
                    .ifPresent(a -> a.setDefault(true, clock.instant()));
        }
    }

    private Address load(UUID userId, UUID addressId) {
        return addresses.findByIdAndUserId(addressId, userId).orElseThrow(() -> AppException.notFound("Address"));
    }
}
