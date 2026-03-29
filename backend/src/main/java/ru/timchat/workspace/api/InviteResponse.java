package ru.timchat.workspace.api;

import java.time.Instant;
import java.util.UUID;

public record InviteResponse(
    UUID id,
    UUID workspaceId,
    String code,
    Instant expiresAt,
    Instant createdAt
) {
}
