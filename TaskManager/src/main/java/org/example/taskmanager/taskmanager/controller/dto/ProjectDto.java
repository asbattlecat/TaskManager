package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ProjectDto(@NotNull UUID id,
                         @NotNull UUID workspaceId,
                         @NotNull String name,
                         String description,
                         @NotNull Instant createdAt,
                         Instant updatedAt,
                         @NotNull boolean archived) {
}
