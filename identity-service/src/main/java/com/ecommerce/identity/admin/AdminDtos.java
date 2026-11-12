package com.ecommerce.identity.admin;

import com.ecommerce.identity.user.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record AssignRoleRequest(@NotBlank @Size(max = 32) String role) {
    }

    public record ChangeStatusRequest(@NotNull UserStatus status, @NotBlank @Size(max = 500) String reason) {
    }

    public record RoleResponse(String code, String description) {
    }
}
