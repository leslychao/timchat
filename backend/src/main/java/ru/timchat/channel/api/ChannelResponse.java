package ru.timchat.channel.api;

import java.time.Instant;
import java.util.UUID;
import ru.timchat.channel.domain.ChannelType;

public record ChannelResponse(
    UUID id,
    UUID workspaceId,
    String name,
    ChannelType type,
    int position,
    Instant createdAt,
    Instant updatedAt
) {
}
