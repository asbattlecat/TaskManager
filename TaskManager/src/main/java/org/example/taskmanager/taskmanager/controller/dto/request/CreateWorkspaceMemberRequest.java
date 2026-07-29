package org.example.taskmanager.taskmanager.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;

import java.util.UUID;

public record CreateWorkspaceMemberRequest(@NotNull UUID workspaceId,
                                           @NotNull UUID userId,
                                           @NotNull WorkspaceRole role) {
}
