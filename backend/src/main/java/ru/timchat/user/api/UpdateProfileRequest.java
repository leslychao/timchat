package ru.timchat.user.api;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 100) String displayName,
    @Size(max = 500) String avatarUrl,
    @Size(max = 200) String statusText
) {
}
