package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.ProjectDto;
import org.example.taskmanager.taskmanager.controller.dto.WorkspaceDto;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.domain.entity.Workspace;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.ProjectMapper;
import org.example.taskmanager.taskmanager.mapper.WorkspaceMapper;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WorkspaceServiceImpl implements WorkspaceService {
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMapper workspaceMapper;
  private final ProjectMapper projectMapper;

  public WorkspaceServiceImpl(WorkspaceRepository workspaceRepository, WorkspaceMapper workspaceMapper,
                              ProjectMapper projectMapper) {
    this.workspaceRepository = workspaceRepository;
    this.workspaceMapper = workspaceMapper;
    this.projectMapper = projectMapper;
  }

  @Override
  public WorkspaceDto create(String name, String description, UUID ownerId) {
    Workspace workspace = new Workspace(name, description, ownerId);
    workspaceRepository.save(workspace);

    return workspaceMapper.toDto(workspace);
  }

  @Override
  public List<ProjectDto> getProjects(UUID workspaceId) {
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new NotFoundException("Workspace not found");
    }

    List<Project> projects = workspaceRepository.findAllByWorkspaceId(workspaceId);
    return projects.stream().map(projectMapper::toDto).toList();
  }

  @Override
  public WorkspaceDto changeName(UUID workspaceId, String newName) {
    Optional<Workspace> workspaceOptional = workspaceRepository.findById(workspaceId);

    if (workspaceOptional.isEmpty()) {
      throw new NotFoundException("Workspace not found");
    }

    Workspace workspace = workspaceOptional.get();
    workspace.setName(newName);
    workspaceRepository.save(workspace);

    return workspaceMapper.toDto(workspace);
  }

  @Override
  public WorkspaceDto changeDescription(UUID workspaceId, String newDescription) {
    Optional<Workspace> workspaceOptional = workspaceRepository.findById(workspaceId);

    if (workspaceOptional.isEmpty()) {
      throw new NotFoundException("Workspace not found");
    }

    Workspace workspace = workspaceOptional.get();
    workspace.setDescription(newDescription);
    workspaceRepository.save(workspace);

    return workspaceMapper.toDto(workspace);
  }
}
