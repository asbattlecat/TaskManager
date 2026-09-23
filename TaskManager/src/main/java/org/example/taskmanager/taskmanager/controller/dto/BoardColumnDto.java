package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BoardColumnDto(
        @NotNull UUID id,
        @NotNull UUID boardId,
        @NotNull String name,
        @NotNull Integer position
) {
}
