package com.ecommerce.identity.admin;

import com.ecommerce.common.audit.AuditService;
import com.ecommerce.common.response.ApiResponse;
import com.ecommerce.common.response.PageRequests;
import com.ecommerce.common.response.PageResponse;
import com.ecommerce.common.security.Roles;
import com.ecommerce.identity.user.UserResponse;
import com.ecommerce.identity.user.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Quản trị người dùng & phân quyền. Chặn 2 lớp: URL (SecurityConfig) + @PreAuthorize. */
@RestController
@RequestMapping("/api/v1/admin/iam")
@PreAuthorize("hasRole('" + Roles.ADMIN + "')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;
    private final AuditService auditService;

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> search(@RequestParam(required = false) String q,
                                                          @RequestParam(required = false) UserStatus status,
                                                          @RequestParam(required = false) String role,
                                                          @RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        return ApiResponse.success(service.search(q, status, role,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(service.get(id));
    }

    @GetMapping("/roles")
    public ApiResponse<List<AdminDtos.RoleResponse>> roles() {
        return ApiResponse.success(service.roles());
    }

    @PostMapping("/users/{id}/roles")
    public ApiResponse<UserResponse> assignRole(@PathVariable UUID id,
                                                @Valid @RequestBody AdminDtos.AssignRoleRequest request) {
        return ApiResponse.success(service.assignRole(id, request.role()));
    }

    @DeleteMapping("/users/{id}/roles/{role}")
    public ApiResponse<UserResponse> revokeRole(@PathVariable UUID id, @PathVariable String role) {
        return ApiResponse.success(service.revokeRole(id, role));
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<UserResponse> changeStatus(@PathVariable UUID id,
                                                  @Valid @RequestBody AdminDtos.ChangeStatusRequest request) {
        return ApiResponse.success(service.changeStatus(id, request));
    }

    @PostMapping("/users/{id}/revoke-sessions")
    public ApiResponse<Map<String, Integer>> revokeSessions(@PathVariable UUID id) {
        return ApiResponse.success(Map.of("revokedSessions", service.revokeSessions(id)));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditService.AuditEntry>> auditLogs(
            @RequestParam(required = false) String targetType, @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String actorId, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(auditService.search(targetType, targetId, actorId,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }
}
