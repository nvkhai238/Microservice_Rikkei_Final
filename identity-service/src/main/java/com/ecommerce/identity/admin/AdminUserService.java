package com.ecommerce.identity.admin;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.response.PageResponse;
import com.ecommerce.common.security.CurrentUser;
import com.ecommerce.common.security.Roles;
import com.ecommerce.identity.auth.SessionService;
import com.ecommerce.identity.user.User;
import com.ecommerce.identity.user.UserRepository;
import com.ecommerce.identity.user.UserResponse;
import com.ecommerce.identity.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Quản trị IAM (FR-IAM-019, FR-ADM-002): chỉ ADMIN; mọi thay đổi quyền/trạng thái đều audit.
 * Lưu ý: role mới có hiệu lực ở access token KẾ TIẾP (tối đa 15 phút) vì token cũ đã ký sẵn role cũ.
 * Muốn có hiệu lực ngay: thu hồi phiên để user phải refresh/đăng nhập lại.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final SessionService sessions;
    private final AuditService audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String q, UserStatus status, String role, Pageable pageable) {
        String keyword = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        return PageResponse.of(users.search(keyword, status, role, pageable), UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(load(id));
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.RoleResponse> roles() {
        return roles.findAll();
    }

    @Transactional
    public UserResponse assignRole(UUID userId, String role) {
        String code = role.trim().toUpperCase(Locale.ROOT);
        if (!roles.exists(code)) {
            throw AppException.validation("role", "UNKNOWN_ROLE");
        }
        User user = load(userId);
        List<String> before = sortedRoles(user);
        if (user.addRole(code, clock.instant())) {
            audit.record("ROLE_ASSIGNED", "USER", userId, Map.of("roles", before), Map.of("roles", sortedRoles(user)));
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse revokeRole(UUID userId, String role) {
        String code = role.trim().toUpperCase(Locale.ROOT);
        if (Roles.ADMIN.equals(code) && userId.equals(CurrentUser.id())) {
            // Tránh tự khóa mình khỏi hệ thống
            throw AppException.businessRule("CANNOT_REVOKE_OWN_ADMIN", "You cannot revoke your own ADMIN role");
        }
        User user = load(userId);
        List<String> before = sortedRoles(user);
        if (user.removeRole(code, clock.instant())) {
            sessions.revokeAll(userId, "ROLE_REVOKED");
            audit.record("ROLE_REVOKED", "USER", userId, Map.of("roles", before), Map.of("roles", sortedRoles(user)));
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse changeStatus(UUID userId, AdminDtos.ChangeStatusRequest req) {
        if (userId.equals(CurrentUser.id())) {
            throw AppException.businessRule("CANNOT_CHANGE_OWN_STATUS", "You cannot change your own status");
        }
        User user = load(userId);
        UserStatus before = user.getStatus();
        user.changeStatus(req.status(), clock.instant());
        if (req.status() == UserStatus.DISABLED) {
            sessions.revokeAll(userId, "ADMIN_DISABLED");
        }
        audit.record("USER_STATUS_CHANGED", "USER", userId, Map.of("status", before),
                Map.of("status", req.status(), "reason", req.reason()));
        return UserResponse.from(user);
    }

    /** FR-IAM-012: quản trị viên thu hồi toàn bộ phiên khi có rủi ro. */
    @Transactional
    public int revokeSessions(UUID userId) {
        load(userId);
        int revoked = sessions.revokeAll(userId, "ADMIN_REVOKED");
        audit.record("SESSIONS_REVOKED", "USER", userId, null, Map.of("revokedSessions", revoked));
        return revoked;
    }

    private User load(UUID id) {
        return users.findById(id).orElseThrow(() -> AppException.notFound("User"));
    }

    private static List<String> sortedRoles(User user) {
        return user.getRoles().stream().sorted().toList();
    }
}
