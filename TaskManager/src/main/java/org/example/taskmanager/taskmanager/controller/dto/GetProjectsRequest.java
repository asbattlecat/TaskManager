package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetProjectsRequest(@NotNull UUID workspaceId) {
}
