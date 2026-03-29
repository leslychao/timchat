package ru.timchat.channel.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.timchat.channel.domain.ChannelType;

public record CreateChannelRequest(
    @NotBlank(message = "{validation.channel.name-required}")
    @Size(max = 100, message = "{validation.channel.name-too-long}")
    String name,

    @NotNull(message = "{validation.channel.type-required}")
    ChannelType type
) {
}
