package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.*;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;

import java.util.List;
import java.util.UUID;

public interface WorkspaceMemberService {
  WorkspaceMemberDto createMember(UUID workspaceId, UUID userId, WorkspaceRole role);
  WorkspaceMemberDto deleteMember(UUID memberId, UUID workspaceId, UUID userId);
  WorkspaceMemberDto setRoleToMember(UUID workspaceMemberId, WorkspaceRole role);
  List<WorkspaceMemberDto> getMembers(UUID workspaceId);
}
