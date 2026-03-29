package ru.timchat.user.api;

import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String username,
    String email,
    String displayName,
    String avatarUrl,
    String statusText
) {
}
