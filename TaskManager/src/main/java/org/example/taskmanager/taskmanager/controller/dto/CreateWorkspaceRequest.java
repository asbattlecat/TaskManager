package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateWorkspaceRequest(@NotNull String name,
                                     String description,
                                     @NotNull UUID ownerId) {
}
