package com.ecommerce.identity.token;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM OneTimeToken t WHERE t.tokenHash = :hash AND t.purpose = :purpose")
    Optional<OneTimeToken> findForUpdate(@Param("hash") String hash, @Param("purpose") OneTimeToken.Purpose purpose);

    /** Cấp token mới thì vô hiệu các token cũ chưa dùng cùng mục đích */
    @Modifying
    @Query("""
            UPDATE OneTimeToken t SET t.usedAt = :now
            WHERE t.userId = :userId AND t.purpose = :purpose AND t.usedAt IS NULL
            """)
    int invalidateActive(@Param("userId") UUID userId, @Param("purpose") OneTimeToken.Purpose purpose,
                         @Param("now") Instant now);
}
