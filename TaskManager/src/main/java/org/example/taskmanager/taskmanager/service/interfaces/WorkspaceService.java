package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.*;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;

import java.util.List;
import java.util.UUID;

public interface WorkspaceService {
  WorkspaceDto create(String name, String description, UUID ownerId);
  WorkspaceDto archive(UUID workspaceId, ArchiveReason reason);
  WorkspaceDto unarchive(UUID workspaceId, UnarchiveReason reason);
  List<ProjectDto> getProjects(UUID workspaceId);
  WorkspaceDto changeName(UUID workspaceId, String newName);
  WorkspaceDto changeDescription(UUID workspaceId, String newDescription);
}
