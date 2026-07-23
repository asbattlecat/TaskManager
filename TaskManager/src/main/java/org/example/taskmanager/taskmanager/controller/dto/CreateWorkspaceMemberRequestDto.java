package org.example.taskmanager.taskmanager.controller.dto;

import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;

import java.util.UUID;

public record CreateWorkspaceMemberRequestDto(UUID workspaceId, UUID userId, WorkspaceRole role) {
}
