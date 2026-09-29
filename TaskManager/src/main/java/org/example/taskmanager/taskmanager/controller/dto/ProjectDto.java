package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;

import java.time.Instant;
import java.util.UUID;

public record ProjectDto(@NotNull UUID id,
                         @NotNull ArchiveState archiveState,
                         @NotNull UUID workspaceId,
                         @NotNull String name,
                         String description,
                         @NotNull Instant createdAt,
                         Instant updatedAt,
                         boolean archived) {
}
