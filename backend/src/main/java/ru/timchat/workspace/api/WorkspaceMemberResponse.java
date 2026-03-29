package ru.timchat.workspace.api;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
    UUID id,
    UUID workspaceId,
    UUID userId,
    Instant joinedAt
) {
}
