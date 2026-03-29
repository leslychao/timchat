package ru.timchat.message.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditMessageRequest(
    @NotBlank(message = "{validation.message.content-required}")
    @Size(max = 4000, message = "{validation.message.content-too-long}")
    String content
) {
}
