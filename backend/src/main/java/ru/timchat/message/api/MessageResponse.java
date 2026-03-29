package ru.timchat.message.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    UUID channelId,
    UUID authorId,
    String authorName,
    String content,
    boolean deleted,
    Instant createdAt,
    Instant updatedAt,
    List<UUID> attachmentIds
) {
}
