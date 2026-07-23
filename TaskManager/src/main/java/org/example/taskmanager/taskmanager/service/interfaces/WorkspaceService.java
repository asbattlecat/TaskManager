package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.*;
import org.example.taskmanager.taskmanager.domain.entity.Project;

import java.util.List;
import java.util.UUID;

public interface WorkspaceService {
  WorkspaceDto create(CreateWorkspaceRequestDto requestDto);
  WorkspaceDto update(UUID id, WorkspaceDto requestDto);
  WorkspaceDto addMember();
  void setRole();
  List<ProjectDto> getProjects();
  List<WorkspaceMemberDto> getMembers();
}
