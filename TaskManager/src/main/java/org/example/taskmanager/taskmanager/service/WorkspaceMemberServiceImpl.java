package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.CreateWorkspaceMemberRequestDto;
import org.example.taskmanager.taskmanager.repository.UserRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceMemberService;

import java.util.UUID;

public class WorkspaceMemberServiceImpl implements WorkspaceMemberService {
  private final UserRepository userRepository;
  private final WorkspaceRepository workspaceRepository;

  public WorkspaceMemberServiceImpl(UserRepository userRepository, WorkspaceRepository workspaceRepository) {
    this.userRepository = userRepository;
    this.workspaceRepository = workspaceRepository;
  }

  @Override
  public void createMember(CreateWorkspaceMemberRequestDto requestDto) {

  }

  @Override
  public void deleteMember(UUID workspaceId, UUID userId) {

  }
}
