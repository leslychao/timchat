package ru.timchat.workspace.api;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
    UUID id,
    String name,
    String slug,
    UUID ownerId,
    Instant createdAt,
    Instant updatedAt
) {
}
