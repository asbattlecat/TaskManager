package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeleteWorkspaceMemberRequest(@NotNull UUID memberId,
                                           @NotNull UUID workspaceId,
                                           @NotNull UUID userId) {
}
