package ru.timchat.workspace.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 100)
    @Pattern(regexp = "^[a-z0-9-]+$",
        message = "{validation.workspace.slug-format}")
    String slug
) {
}
