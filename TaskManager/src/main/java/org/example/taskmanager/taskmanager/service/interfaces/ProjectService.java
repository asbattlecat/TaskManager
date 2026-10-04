package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.response.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.response.ProjectDto;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
  ProjectDto create(UUID workspaceId, String name, String description);
  ProjectDto archive(UUID projectId, ArchiveReason archiveReason);
  List<ProjectDto> archiveByWorkspace(UUID workspaceId, ArchiveReason reason);
  ProjectDto unarchive(UUID projectId, UnarchiveReason reason);
  List<ProjectDto> unarchiveByWorkspace(UUID workspaceId, UnarchiveReason reason);
  List<BoardDto> getProjectBoards(UUID projectId);
  ProjectDto changeName(UUID projectId, String newName);
  ProjectDto changeDescription(UUID projectId, String newDescription);
}
