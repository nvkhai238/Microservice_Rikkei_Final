package com.ecommerce.catalog.brand;

import com.ecommerce.common.persistence.AssignedIdEntity;
import com.ecommerce.catalog.support.ActiveStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "brands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends AssignedIdEntity {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String slug;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActiveStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public Brand(String name, String slug, Instant now) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.slug = slug;
        this.status = ActiveStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String slug, Instant now) {
        this.name = name;
        this.slug = slug;
        this.updatedAt = now;
    }

    public void changeStatus(ActiveStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }
}
