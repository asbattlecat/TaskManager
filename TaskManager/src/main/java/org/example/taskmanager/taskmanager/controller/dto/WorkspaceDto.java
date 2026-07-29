package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceDto(@NotNull UUID id,
                           @NotNull String name,
                           String description,
                           @NotNull UUID ownerId,
                           @NotNull Instant createdAt,
                           Instant updatedAt) {
}
