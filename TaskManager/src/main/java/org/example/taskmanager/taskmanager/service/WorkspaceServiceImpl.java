package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.response.ProjectDto;
import org.example.taskmanager.taskmanager.controller.dto.response.WorkspaceDto;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEvent;
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

  public WorkspaceServiceImpl(
          ProjectRepository projectRepository,
          WorkspaceRepository workspaceRepository,
          WorkspaceMapper workspaceMapper,
          ProjectMapper projectMapper,
          DomainEventPublisher domainEventPublisher
  ) {
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
    Workspace workspace = getWorkspace(workspaceId, "archive");

    if (workspace.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalStateException("Workspace with id " + workspaceId + " is already archived");

    if (reason == ArchiveReason.PARENT_ARCHIVED)
      throw new IllegalArgumentException("Workspace with id " + workspaceId
              + " cannot be archived by cascade mechanics");

    workspace.archive(reason);
    workspaceRepository.save(workspace);
    workspace.getEvents().forEach(domainEventPublisher::publish);
    workspace.clearEvents();

    return workspaceMapper.toDto(workspace);
  }

  @Override
  public WorkspaceDto unarchive(UUID workspaceId, UnarchiveReason reason) {
    Workspace workspace = getWorkspace(workspaceId, "unarchive");

    if (workspace.getArchiveState() == ArchiveState.ACTIVE)
      throw new IllegalStateException("Workspace with id " + workspaceId + " is already active");
    if (reason == UnarchiveReason.CASCADE)
      throw new IllegalArgumentException("Workspace with id "
              + workspaceId + " cannot be archived by cascade mechanics");

    workspace.unarchive(reason);
    workspaceRepository.save(workspace);
    workspace.getEvents().forEach(domainEventPublisher::publish);
    workspace.clearEvents();

    return workspaceMapper.toDto(workspace);
  }

  @Override
  public List<ProjectDto> getProjects(UUID workspaceId) {
    Workspace workspace = getWorkspace(workspaceId, "getProjects");

    isWorkspaceActive(workspace, "getProjects");

    List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);
    if (projects.isEmpty())
      throw new NotFoundException("Projects with workspaceId as parent id "
              + workspaceId + " not found");

    return projects.stream().map(projectMapper::toDto).toList();
  }

  @Override
  public WorkspaceDto changeName(UUID workspaceId, String newName) {
    Workspace workspace = getWorkspace(workspaceId, "changeName");

    isWorkspaceActive(workspace, "changeName");

    workspace.setName(newName);
    workspaceRepository.save(workspace);

    return workspaceMapper.toDto(workspace);
  }

  @Override
  public WorkspaceDto changeDescription(UUID workspaceId, String newDescription) {
    Workspace workspace = getWorkspace(workspaceId, "changeDescription");

    isWorkspaceActive(workspace, "changeDescription");

    workspace.setDescription(newDescription);
    workspaceRepository.save(workspace);

    return workspaceMapper.toDto(workspace);
  }

  private Workspace getWorkspace(UUID workspaceId, String operationName) {
    return workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace with id "
                    + workspaceId + " not found during " + operationName + "operation"));
  }

  private void isWorkspaceActive(Workspace workspace, String operationName) {
    if (workspace.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalStateException("Cannot do " + operationName
              + " operation because workspace with id " + workspace.getId() + " is archived");
  }
}
