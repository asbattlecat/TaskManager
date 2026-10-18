package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.ProjectDto;
import org.example.taskmanager.taskmanager.controller.dto.response.WorkspaceDto;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.domain.entity.Workspace;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;
import org.example.taskmanager.taskmanager.domain.records.CascadeArchiveResult;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEventPublisher;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.ProjectMapper;
import org.example.taskmanager.taskmanager.mapper.WorkspaceMapper;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.UserRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.CascadeArchiveService;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceMemberService;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class WorkspaceServiceImpl implements WorkspaceService {
  private final WorkspaceMemberService workspaceMemberService;
  private final CascadeArchiveService cascadeArchiveService;
  private final DomainEventPublisher domainEventPublisher;
  private final WorkspaceRepository workspaceRepository;
  private final ProjectRepository projectRepository;
  private final WorkspaceMapper workspaceMapper;
  private final UserRepository userRepository;
  private final ProjectMapper projectMapper;


  public WorkspaceServiceImpl(
          WorkspaceMemberService workspaceMemberService,
          CascadeArchiveService cascadeArchiveService,
          DomainEventPublisher domainEventPublisher,
          WorkspaceRepository workspaceRepository,
          ProjectRepository projectRepository,
          WorkspaceMapper workspaceMapper,
          UserRepository userRepository,
          ProjectMapper projectMapper
  ) {
    this.workspaceMemberService = workspaceMemberService;
    this.cascadeArchiveService = cascadeArchiveService;
    this.domainEventPublisher = domainEventPublisher;
    this.workspaceRepository = workspaceRepository;
    this.projectRepository = projectRepository;
    this.workspaceMapper = workspaceMapper;
    this.userRepository = userRepository;
    this.projectMapper = projectMapper;
  }

  @Transactional
  @Override
  public WorkspaceDto create(String name, String description, UUID ownerId) {
    log.info("creating workspace, name={}, description={}, ownerId={}", name, description, ownerId);

    // Правило агрегата: workspace имеет OWNER. Поэтому создаем owner-а
    if (!userRepository.existsById(ownerId)) {
      log.error("user not found, userId={}", ownerId);
      throw new NotFoundException("User with id " + ownerId
              + " not found during workspace create operation");
    }

    Workspace workspace = new Workspace(name, description, ownerId);
    workspaceRepository.save(workspace);

    // создателя рабочего пространства делаем админом
    workspaceMemberService.create(workspace.getId(), ownerId, WorkspaceRole.ADMIN);

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

    CascadeArchiveResult result = cascadeArchiveService.archiveByWorkspace(workspaceId);
    log.debug("workspace archive completed, workspaceId={}, cascaded projects={}, boards={}, tasks={}",
            workspaceId,
            result.projectsArchived(),
            result.boardsArchived(),
            result.tasksArchived()
    );
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
    log.info("workspace getProjects operation starts, workspaceId={}", workspaceId);

    Workspace workspace = getWorkspace(workspaceId, "getProjects");
    isWorkspaceActive(workspace, "getProjects");
    List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);

    log.debug("workspace getProjects operation completed, workspaceId={}", workspaceId);
    return projects.stream().map(projectMapper::toDto).toList();
  }

  @Transactional
  @Override
  public WorkspaceDto changeName(UUID workspaceId, String newName) {
    log.info("workspace changeName operation starts, workspaceId={}, newName={}", workspaceId, newName);
    Workspace workspace = getWorkspace(workspaceId, "changeName");

    isWorkspaceActive(workspace, "changeName");

    workspace.setName(newName);
    workspaceRepository.save(workspace);

    log.debug("workspace changeName operation completed, workspaceId={}, newName={}", workspaceId, newName);
    return workspaceMapper.toDto(workspace);
  }

  @Transactional
  @Override
  public WorkspaceDto changeDescription(UUID workspaceId, String newDescription) {
    log.info("workspace changeDescription operation starts, workspaceId={}, newDescription={}",
            workspaceId, newDescription);
    Workspace workspace = getWorkspace(workspaceId, "changeDescription");

    isWorkspaceActive(workspace, "changeDescription");

    workspace.setDescription(newDescription);
    workspaceRepository.save(workspace);

    log.debug("workspace changeDescription operation completed, workspaceId={}, newDescription={}",
            workspaceId, newDescription);
    return workspaceMapper.toDto(workspace);
  }

  private Workspace getWorkspace(UUID workspaceId, String operationName) {
    return workspaceRepository
            .findById(workspaceId)
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
