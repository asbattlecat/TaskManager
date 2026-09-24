package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CommentDto(
        @NotNull UUID id,
        @NotNull UUID taskId,
        @NotNull UUID authorId,
        @NotNull String content,
        @NotNull Instant createdAt,
        Instant updatedAt
) {
}
