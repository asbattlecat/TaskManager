package org.example.taskmanager.taskmanager.service.interfaces;

import jakarta.validation.constraints.NotNull;
import org.example.taskmanager.taskmanager.controller.dto.*;

import java.util.List;
import java.util.UUID;

public interface WorkspaceService {
  WorkspaceDto create(String name, String description, UUID ownerId);
  List<ProjectDto> getProjects(UUID workspaceId);
}
