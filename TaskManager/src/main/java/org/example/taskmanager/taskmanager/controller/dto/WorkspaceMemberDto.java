package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;

import java.io.Serializable;
import java.util.UUID;

public record WorkspaceMemberDto(@NotNull UUID id,
                                 @NotNull UUID workspaceId,
                                 @NotNull UUID userId,
                                 @NotNull WorkspaceRole role) {
}
