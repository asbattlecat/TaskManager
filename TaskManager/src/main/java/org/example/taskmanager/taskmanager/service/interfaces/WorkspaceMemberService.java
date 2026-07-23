package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.CreateWorkspaceMemberRequestDto;

import java.util.UUID;

public interface WorkspaceMemberService {
  void createMember(CreateWorkspaceMemberRequestDto requestDto);
  void deleteMember(UUID workspaceId, UUID userId);
}
