package org.example.taskmanager.taskmanager.controller.dto.response;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AuditEntityDto(
        @NotNull UUID id,
        @NotNull UUID taskId,
        @NotNull String fieldName,
        @NotNull String oldValue,
        @NotNull String newValue,
        @NotNull Instant timestamp
) {
}
