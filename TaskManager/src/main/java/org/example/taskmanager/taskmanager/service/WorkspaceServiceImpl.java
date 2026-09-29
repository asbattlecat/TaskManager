package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.ProjectDto;
import org.example.taskmanager.taskmanager.controller.dto.WorkspaceDto;
import org.example.taskmanager.taskmanager.controller.dto.event.DomainEvent;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.domain.entity.Workspace;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEventPublisher;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
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

  private final DomainEventPublisher domainEventPublisher;

  public WorkspaceServiceImpl(ProjectRepository projectRepository, WorkspaceRepository workspaceRepository,
                              WorkspaceMapper workspaceMapper, ProjectMapper projectMapper,
                              DomainEventPublisher domainEventPublisher) {
    this.projectRepository = projectRepository;
    this.workspaceRepository = workspaceRepository;
    this.workspaceMapper = workspaceMapper;
    this.projectMapper = projectMapper;
    this.domainEventPublisher = domainEventPublisher;
  }

  @Override
  public WorkspaceDto create(String name, String description, UUID ownerId) {
    Workspace workspace = new Workspace(name, description, ownerId);
    workspaceRepository.save(workspace);

    return workspaceMapper.toDto(workspace);
  }

  @Override
  public WorkspaceDto archive(UUID workspaceId, ArchiveReason reason) {
    Workspace workspace = getWorkspace(workspaceId);

    if (workspace.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalStateException("Workspace is already archived");

    if (reason == ArchiveReason.PARENT_ARCHIVED)
      throw new IllegalArgumentException("Workspace cannot be archived by cascade mechanics");

    workspace.archive(reason);
    List<DomainEvent> events = workspace.getEvents();
    workspace.clearEvents();
    workspaceRepository.save(workspace);

    // запускаем каскад по цепочке ниже
    events.forEach(domainEventPublisher::publish);

    return workspaceMapper.toDto(workspace);
  }

  @Override
  public WorkspaceDto unarchive(UUID workspaceId, UnarchiveReason reason) {
    Workspace workspace = getWorkspace(workspaceId);

    if (workspace.getArchiveState() == ArchiveState.ACTIVE)
      throw new IllegalStateException("Workspace is already active");
    if (reason == UnarchiveReason.CASCADE)
      throw new IllegalArgumentException("Workspace cannot be archived by cascade mechanics");

    workspace.unarchive(reason);
    List<DomainEvent> events = workspace.getEvents();
    workspace.clearEvents();
    workspaceRepository.save(workspace);

    // запускаем каскад по цепочке ниже
    events.forEach(domainEventPublisher::publish);

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
    Workspace workspace = getWorkspace(workspaceId);

    workspace.setName(newName);
    workspaceRepository.save(workspace);

    return workspaceMapper.toDto(workspace);
  }

  @Override
  public WorkspaceDto changeDescription(UUID workspaceId, String newDescription) {
    Workspace workspace = getWorkspace(workspaceId);

    workspace.setDescription(newDescription);
    workspaceRepository.save(workspace);

    return workspaceMapper.toDto(workspace);
  }

  private Workspace getWorkspace(UUID workspaceId) {
    return workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found"));
  }
}
