package org.example.taskmanager.taskmanager.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;

import java.util.UUID;

public record ChangeWorkspaceMemberRoleRequest(@NotNull UUID workspaceMemberId,
                                               @NotNull WorkspaceRole role) {
}
