package ru.timchat.workspace.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
    @Size(max = 100) String name,
    @Size(max = 100)
    @Pattern(regexp = "^[a-z0-9-]+$",
        message = "{validation.workspace.slug-format}")
    String slug
) {
}
