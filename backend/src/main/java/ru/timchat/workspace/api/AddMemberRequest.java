package ru.timchat.workspace.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddMemberRequest(
    @NotNull UUID userId
) {
}
