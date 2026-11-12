package com.ecommerce.identity.address;

import com.ecommerce.common.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "recipient_name", nullable = false)
    private String recipientName;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    private String line1;
    @Column(nullable = false)
    private String ward;
    @Column(nullable = false)
    private String district;
    @Column(nullable = false)
    private String province;
    @Column(name = "postal_code")
    private String postalCode;
    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public Address(UUID userId, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.createdAt = now;
    }

    public void apply(AddressDtos.AddressRequest r, String normalizedPhone, Instant now) {
        this.recipientName = r.recipientName().trim();
        this.phone = normalizedPhone;
        this.line1 = r.line1().trim();
        this.ward = r.ward().trim();
        this.district = r.district().trim();
        this.province = r.province().trim();
        this.postalCode = r.postalCode() == null || r.postalCode().isBlank() ? null : r.postalCode().trim();
        this.updatedAt = now;
    }

    public void setDefault(boolean value, Instant now) {
        this.defaultAddress = value;
        this.updatedAt = now;
    }
}
