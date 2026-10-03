package org.example.taskmanager.taskmanager.controller.dto.response;

import jakarta.validation.constraints.NotNull;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;

import java.util.UUID;

public record WorkspaceMemberDto(@NotNull UUID id,
                                 @NotNull UUID workspaceId,
                                 @NotNull UUID userId,
                                 @NotNull WorkspaceRole role) {
}
