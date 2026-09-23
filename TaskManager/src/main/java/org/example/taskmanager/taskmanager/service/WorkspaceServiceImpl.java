package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.ProjectDto;
import org.example.taskmanager.taskmanager.controller.dto.WorkspaceDto;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.domain.entity.Workspace;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.ProjectMapper;
import org.example.taskmanager.taskmanager.mapper.WorkspaceMapper;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {
  private final ProjectRepository projectRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMapper workspaceMapper;
  private final ProjectMapper projectMapper;

  public WorkspaceServiceImpl(ProjectRepository projectRepository, WorkspaceRepository workspaceRepository,
                              WorkspaceMapper workspaceMapper, ProjectMapper projectMapper) {
    this.projectRepository = projectRepository;
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

    List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);
    if (projects.isEmpty()) {
      throw new NotFoundException("Projects not found");
    }

    return projects.stream().map(projectMapper::toDto).toList();
  }

  @Override
  public WorkspaceDto changeName(UUID workspaceId, String newName) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found"));

    workspace.setName(newName);
    workspaceRepository.save(workspace);

    return workspaceMapper.toDto(workspace);
  }

  @Override
  public WorkspaceDto changeDescription(UUID workspaceId, String newDescription) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found"));

    workspace.setDescription(newDescription);
    workspaceRepository.save(workspace);

    return workspaceMapper.toDto(workspace);
  }
}
