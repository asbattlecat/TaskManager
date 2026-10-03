package org.example.taskmanager.taskmanager.controller.dto.response;

import jakarta.validation.constraints.NotNull;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceDto(@NotNull UUID id,
                           @NotNull ArchiveState archiveState,
                           @NotNull String name,
                           String description,
                           @NotNull UUID ownerId,
                           @NotNull Instant createdAt,
                           Instant updatedAt) {
}
