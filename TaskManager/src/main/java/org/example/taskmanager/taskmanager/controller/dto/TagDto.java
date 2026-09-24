package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.example.taskmanager.taskmanager.domain.enums.TagColor;

import java.util.UUID;

public record TagDto(
        @NotNull UUID id,
        @NotNull UUID taskId,
        @NotNull String name,
        @NotNull TagColor color
) {
}
