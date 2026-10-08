package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.ProjectDto;
import org.example.taskmanager.taskmanager.controller.dto.response.WorkspaceDto;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.domain.entity.Workspace;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEventPublisher;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.logs.CustomLogger;
import org.example.taskmanager.taskmanager.mapper.ProjectMapper;
import org.example.taskmanager.taskmanager.mapper.WorkspaceMapper;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class WorkspaceServiceImpl implements WorkspaceService {
  private final DomainEventPublisher domainEventPublisher;
  private final WorkspaceRepository workspaceRepository;
  private final ProjectRepository projectRepository;
  private final WorkspaceMapper workspaceMapper;
  private final ProjectMapper projectMapper;


  public WorkspaceServiceImpl(
          DomainEventPublisher domainEventPublisher,
          WorkspaceRepository workspaceRepository,
          ProjectRepository projectRepository,
          WorkspaceMapper workspaceMapper,
          ProjectMapper projectMapper
  ) {
    this.domainEventPublisher = domainEventPublisher;
    this.workspaceRepository = workspaceRepository;
    this.projectRepository = projectRepository;
    this.workspaceMapper = workspaceMapper;
    this.projectMapper = projectMapper;
  }

  @Transactional
  @Override
  public WorkspaceDto create(String name, String description, UUID ownerId) {
    log.info("creating workspace, name={}, description={}, ownerId={}", name, description, ownerId);

    Workspace workspace = new Workspace(name, description, ownerId);
    workspaceRepository.save(workspace);

    log.debug("workspace created and saved in database, workspaceId={}", workspace.getId());
    return workspaceMapper.toDto(workspace);
  }

  @Transactional
  @Override
  public WorkspaceDto archive(UUID workspaceId, ArchiveReason reason) {
    log.info("workspace archive starts, workspaceId={}, reason={}", workspaceId, reason);
    Workspace workspace = getWorkspace(workspaceId, "archive");

    if (reason == ArchiveReason.PARENT_ARCHIVED) {
      log.error("cannot archive workspace with id={}, reason is incorrect, reason={}", workspaceId, reason);;
      throw new IllegalArgumentException("Workspace with id " + workspaceId
              + " cannot be archived by cascade mechanics");
    }

    workspace.archive(reason);
    workspaceRepository.save(workspace);
    workspace.getEvents().forEach(domainEventPublisher::publish);
    workspace.clearEvents();

    log.debug("workspace archive completed, workspaceId={}, reason={}", workspaceId, reason);
    return workspaceMapper.toDto(workspace);
  }

  @Transactional
  @Override
  public WorkspaceDto unarchive(UUID workspaceId, UnarchiveReason reason) {
    log.info("workspace unarchive starts, workspaceId={}, reason={}", workspaceId, reason);
    Workspace workspace = getWorkspace(workspaceId, "unarchive");

    if (reason == UnarchiveReason.CASCADE) {
      log.error("cannot unarchive workspace with workspaceId={}, reason is incorrect, reason={}", workspaceId, reason);;
      throw new IllegalArgumentException("Workspace with id "
              + workspaceId + " cannot be archived by cascade mechanics");
    }

    workspace.unarchive(reason);
    workspaceRepository.save(workspace);
    workspace.getEvents().forEach(domainEventPublisher::publish);
    workspace.clearEvents();

    log.debug("workspace unarchive completed, id={}, reason={}", workspaceId, reason);
    return workspaceMapper.toDto(workspace);
  }

  @Transactional
  @Override
  public List<ProjectDto> getProjects(UUID workspaceId) {
    CustomLogger.operationStarts("workspace", "getProjects", "workspaceId", workspaceId);

    Workspace workspace = getWorkspace(workspaceId, "getProjects");
    isWorkspaceActive(workspace, "getProjects");
    List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);

    CustomLogger.operationCompleted("workspace", "getProjects", "workspaceId", workspaceId);
    return projects.stream().map(projectMapper::toDto).toList();
  }

  @Transactional
  @Override
  public WorkspaceDto changeName(UUID workspaceId, String newName) {
    CustomLogger.operationStarts("workspace", "changeName", "workspaceId", workspaceId);
    Workspace workspace = getWorkspace(workspaceId, "changeName");

    isWorkspaceActive(workspace, "changeName");

    workspace.setName(newName);
    workspaceRepository.save(workspace);

    CustomLogger.operationCompleted("workspace", "changeName", "workspaceId", workspaceId);
    log.debug("Workspace changeName completed, workspaceId={}", workspaceId);
    return workspaceMapper.toDto(workspace);
  }

  @Transactional
  @Override
  public WorkspaceDto changeDescription(UUID workspaceId, String newDescription) {
    CustomLogger.operationStarts("workspace", "changeDescription", "workspaceId", workspaceId);
    Workspace workspace = getWorkspace(workspaceId, "changeDescription");

    isWorkspaceActive(workspace, "changeDescription");

    workspace.setDescription(newDescription);
    workspaceRepository.save(workspace);


    CustomLogger.operationCompleted("workspace", "changeDescription", "workspaceId", workspaceId);
    return workspaceMapper.toDto(workspace);
  }

  private Workspace getWorkspace(UUID workspaceId, String operationName) {
    return workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> {
              log.error("Workspace not found, id={}, operationName={}", workspaceId, operationName);
              return new NotFoundException("Workspace with id "
                      + workspaceId + " not found during " + operationName + "operation");
            });
  }

  private void isWorkspaceActive(Workspace workspace, String operationName) {
    if (workspace.getArchiveState() != ArchiveState.ACTIVE) {
      log.error("Cannot do {} because workspace is archived, id={}", operationName, workspace.getId());
      throw new IllegalStateException("Cannot do " + operationName
              + " operation because workspace with id " + workspace.getId() + " is archived");
    }
  }
}
