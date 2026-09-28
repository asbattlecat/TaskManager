package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.example.taskmanager.taskmanager.domain.enums.BoardType;

import java.time.Instant;
import java.util.UUID;

public record BoardDto(@NotNull UUID id,
                       boolean archived,
                       @NotNull UUID projectId,
                       @NotNull String name,
                       String description,
                       @NotNull BoardType type,
                       @NotNull Instant createdAt,
                       Instant updatedAt) {
}
