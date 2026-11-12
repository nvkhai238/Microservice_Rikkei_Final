package com.ecommerce.identity.user;

import com.ecommerce.common.persistence.AssignedIdEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tài khoản người dùng. Logic trạng thái (khóa tạm khi sai mật khẩu, kích hoạt...) nằm TRONG entity
 * để không chỗ nào khác sửa field tùy tiện.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AssignedIdEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Role nằm ở bảng user_roles; nạp EAGER vì lần nào phát token cũng cần */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_code")
    private Set<String> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic lock: hai request cùng sửa user => request sau bị từ chối thay vì ghi đè (DR-008) */
    @Version
    private long version;

    public static User register(String normalizedEmail, String passwordHash, String fullName, String phone,
                                boolean verificationRequired, Instant now) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.email = normalizedEmail;
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        user.phone = phone;
        user.status = verificationRequired ? UserStatus.PENDING_VERIFICATION : UserStatus.ACTIVE;
        user.roles.add("CUSTOMER");
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** @return true nếu lần sai này làm tài khoản bị khóa tạm */
    public boolean recordFailedLogin(int maxAttempts, Duration lockDuration, Instant now) {
        failedLoginAttempts++;
        updatedAt = now;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
            failedLoginAttempts = 0;
            return true;
        }
        return false;
    }

    public void recordSuccessfulLogin(Instant now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
        updatedAt = now;
    }

    public void markEmailVerified(Instant now) {
        emailVerified = true;
        if (status == UserStatus.PENDING_VERIFICATION) {
            status = UserStatus.ACTIVE;
        }
        updatedAt = now;
    }

    public void changePassword(String newHash, Instant now) {
        passwordHash = newHash;
        failedLoginAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    public void updateProfile(String fullName, String phone, Instant now) {
        this.fullName = fullName;
        this.phone = phone;
        this.updatedAt = now;
    }

    public void changeStatus(UserStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    public boolean addRole(String role, Instant now) {
        updatedAt = now;
        return roles.add(role);
    }

    public boolean removeRole(String role, Instant now) {
        updatedAt = now;
        return roles.remove(role);
    }

    public void grantAdminBootstrap(Instant now) {
        roles.add("ADMIN");
        emailVerified = true;
        status = UserStatus.ACTIVE;
        updatedAt = now;
    }
}
