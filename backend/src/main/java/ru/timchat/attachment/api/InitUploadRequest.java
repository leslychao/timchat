package ru.timchat.attachment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record InitUploadRequest(
    UUID channelId,
    @NotBlank(message = "{validation.attachment.filename-required}")
    String fileName,
    @NotBlank(message = "{validation.attachment.content-type-required}")
    String contentType,
    @Positive(message = "{validation.attachment.size-positive}")
    long sizeBytes
) {
}
