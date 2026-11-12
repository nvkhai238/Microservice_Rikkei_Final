package com.ecommerce.identity.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(UUID userId);

    /** Luôn tìm theo CẢ id lẫn userId => không bao giờ trả về địa chỉ của người khác (chống IDOR). */
    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Address a SET a.defaultAddress = false WHERE a.userId = :userId AND a.defaultAddress = true")
    int clearDefault(@Param("userId") UUID userId);
}
