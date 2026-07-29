package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.ProjectDto;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
  ProjectDto create(UUID workspaceId, String name, String description);
  ProjectDto delete(UUID projectId);
  ProjectDto archive(UUID projectId);
  ProjectDto unarchive(UUID projectId);
  List<BoardDto> getProjectBoards(UUID projectId);
  ProjectDto changeName(UUID projectId, String newName);
  ProjectDto changeDescription(UUID projectId, String newDescription);
}
