package ru.timchat.attachment.api;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
    UUID id,
    UUID workspaceId,
    UUID channelId,
    String fileName,
    String contentType,
    long sizeBytes,
    String status,
    Instant createdAt
) {
}
