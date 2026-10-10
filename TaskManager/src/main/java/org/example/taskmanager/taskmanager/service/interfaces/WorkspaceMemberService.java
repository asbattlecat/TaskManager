package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.response.WorkspaceMemberDto;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;

import java.util.List;
import java.util.UUID;

public interface WorkspaceMemberService {
  WorkspaceMemberDto create(UUID workspaceId, UUID userId, WorkspaceRole role);
  WorkspaceMemberDto setRoleToMember(UUID workspaceMemberId, WorkspaceRole role);
  List<WorkspaceMemberDto> getMembersOfWorkspace(UUID workspaceId);
  WorkspaceMemberDto delete(UUID workspaceMemberId);
  List<WorkspaceMemberDto> archiveByUser(UUID userId);
  List<WorkspaceMemberDto> unarchiveByUser(UUID userId);
  WorkspaceMemberDto blockByManager(UUID workspaceMemberId);
  WorkspaceMemberDto unblockByManager(UUID workspaceMemberId);
}
