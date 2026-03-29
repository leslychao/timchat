package ru.timchat.common.error;

public record ErrorResponse(
    String code,
    String message,
    String details,
    String traceId
) {
}
