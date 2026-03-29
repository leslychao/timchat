package ru.timchat.channel.api;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderChannelsRequest(
    @NotEmpty(message = "{validation.channel.order-required}")
    List<UUID> channelIds
) {
}
