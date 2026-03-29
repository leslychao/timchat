package ru.timchat.permission.api;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(
    @NotBlank(message = "{validation.role.name-required}")
    String roleName
) {
}
