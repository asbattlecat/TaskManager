package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.example.taskmanager.taskmanager.domain.enums.TaskPriority;
import org.example.taskmanager.taskmanager.domain.enums.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskDto(
        @NotNull UUID id,
        @NotNull  UUID boardId,
        UUID columnId,
        @NotNull String name,
        String description,
        @NotNull TaskStatus status,
        @NotNull TaskPriority priority,
        UUID assigneeId,
        @NotNull UUID creatorId,
        @NotNull Instant createdAt,
        Instant updatedAt
) {
}
