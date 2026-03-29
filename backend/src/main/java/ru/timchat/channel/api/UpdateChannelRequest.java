package ru.timchat.channel.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChannelRequest(
    @NotBlank(message = "{validation.channel.name-required}")
    @Size(max = 100, message = "{validation.channel.name-too-long}")
    String name
) {
}
