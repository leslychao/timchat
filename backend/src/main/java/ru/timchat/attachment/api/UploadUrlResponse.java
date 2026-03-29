package ru.timchat.attachment.api;

import java.util.UUID;

public record UploadUrlResponse(
    UUID attachmentId,
    String uploadUrl
) {
}
