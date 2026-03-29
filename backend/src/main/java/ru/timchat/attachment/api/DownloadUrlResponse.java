package ru.timchat.attachment.api;

import java.util.UUID;

public record DownloadUrlResponse(
    UUID attachmentId,
    String downloadUrl,
    String fileName,
    String contentType
) {
}
