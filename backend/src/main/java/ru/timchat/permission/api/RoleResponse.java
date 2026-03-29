package ru.timchat.permission.api;

import java.util.UUID;

public record RoleResponse(
    UUID id,
    String name,
    boolean system
) {
}
