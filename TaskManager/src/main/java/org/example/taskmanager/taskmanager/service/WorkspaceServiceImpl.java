package org.example.taskmanager.taskmanager.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.ProjectDto;
import org.example.taskmanager.taskmanager.controller.dto.response.WorkspaceDto;
import org.example.taskmanager.taskmanager.domain.aggregate.Project;
import org.example.taskmanager.taskmanager.domain.aggregate.Workspace;
import org.example.taskmanager.taskmanager.domain.enums.*;
import org.example.taskmanager.taskmanager.domain.records.CascadeArchiveResult;
import org.example.taskmanager.taskmanager.domain.records.CascadeUnarchiveResult;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.ProjectMapper;
import org.example.taskmanager.taskmanager.mapper.WorkspaceMapper;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.UserRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.CascadeArchiveService;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceAccessChecker;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceMemberService;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Validated
public class WorkspaceServiceImpl implements WorkspaceService {
  private final WorkspaceAccessChecker workspaceAccessChecker;
  private final WorkspaceMemberService workspaceMemberService;
  private final CascadeArchiveService cascadeArchiveService;
  private final WorkspaceRepository workspaceRepository;
  private final ProjectRepository projectRepository;
  private final WorkspaceMapper workspaceMapper;
  private final UserRepository userRepository;
  private final ProjectMapper projectMapper;


  public WorkspaceServiceImpl(
          WorkspaceAccessChecker workspaceAccessChecker,
          WorkspaceMemberService workspaceMemberService,
          CascadeArchiveService cascadeArchiveService,
          WorkspaceRepository workspaceRepository,
          ProjectRepository projectRepository,
          WorkspaceMapper workspaceMapper,
          UserRepository userRepository,
          ProjectMapper projectMapper
  ) {
    this.workspaceAccessChecker = workspaceAccessChecker;
    this.workspaceMemberService = workspaceMemberService;
    this.cascadeArchiveService = cascadeArchiveService;
    this.workspaceRepository = workspaceRepository;
    this.projectRepository = projectRepository;
    this.workspaceMapper = workspaceMapper;
    this.userRepository = userRepository;
    this.projectMapper = projectMapper;
  }

  @Transactional
  @Override
  public WorkspaceDto create(
          @NotBlank String name,
          String description,
          @NotNull UUID ownerId
  ) {
    log.info("creating workspace, name={}, description={}, ownerId={}", name, description, ownerId);

    // Правило агрегата: workspace имеет OWNER. Поэтому создаем owner-а
    if (!userRepository.existsById(ownerId)) {
      log.error("user not found, userId={}", ownerId);
      throw new NotFoundException("User with id " + ownerId
              + " not found during workspace create operation");
    }

    Workspace workspace = new Workspace(name, description, ownerId);
    workspaceRepository.save(workspace);

    // создателя рабочего пространства делаем OWNER-ом
    workspaceMemberService.create(workspace.getId(), ownerId, WorkspaceRole.OWNER);

    log.debug("workspace created and saved in database, workspaceId={}", workspace.getId());
    return workspaceMapper.toDto(workspace);
  }

  // TODO валидация пользователя
  @Transactional
  @Override
  public WorkspaceDto archive(
          @NotNull UUID workspaceId,
          @NotNull UUID memberId
  ) {
    log.info("workspace archive starts, workspaceId={}", workspaceId);
    workspaceAccessChecker.check(RootEntityType.WORKSPACE, workspaceId, memberId);

    Workspace workspace = getWorkspace(workspaceId, "archive");

    workspace.archive(ArchiveReason.USER_ACTION);
    workspaceRepository.save(workspace);
    CascadeArchiveResult result = cascadeArchiveService.archiveByWorkspace(workspaceId, memberId);

    log.debug("workspace archive completed, workspaceId={}, cascaded projects={}, boards={}, tasks={}",
            workspaceId,
            result.projectsArchived(),
            result.boardsArchived(),
            result.tasksArchived()
    );
    return workspaceMapper.toDto(workspace);
  }

  // TODO валидация пользователя
  @Transactional
  @Override
  public WorkspaceDto unarchive(
          @NotNull UUID workspaceId,
          @NotNull UUID memberId
  ) {
    log.info("workspace unarchive starts, workspaceId={}", workspaceId);
    workspaceAccessChecker.check(RootEntityType.WORKSPACE, workspaceId, memberId);

    Workspace workspace = getWorkspace(workspaceId, "unarchive");

    workspace.unarchive(UnarchiveReason.USER_ACTION);
    workspaceRepository.save(workspace);
    CascadeUnarchiveResult result = cascadeArchiveService.unarchiveByWorkspace(workspaceId, memberId);

    log.debug("workspace unarchive completed, workspaceId={}, cascaded projects={}, boards={}, tasks={}",
            workspaceId,
            result.projectsUnarchived(),
            result.projectsUnarchived(),
            result.projectsUnarchived()
    );
    return workspaceMapper.toDto(workspace);
  }

  @Transactional(readOnly = true)
  @Override
  public List<ProjectDto> getProjects(
          @NotNull UUID workspaceId,
          @NotNull UUID memberId
  ) {
    log.info("workspace getProjects operation starts, workspaceId={}", workspaceId);
    workspaceAccessChecker.check(RootEntityType.WORKSPACE, workspaceId, memberId);

    Workspace workspace = getWorkspace(workspaceId, "getProjects");
    isWorkspaceActive(workspace, "getProjects");
    List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);

    log.debug("workspace getProjects operation completed, workspaceId={}", workspaceId);
    return projects.stream().map(projectMapper::toDto).toList();
  }

  @Transactional
  @Override
  public WorkspaceDto changeName(
          @NotNull UUID workspaceId,
          @NotBlank String newName,
          @NotNull UUID memberId
  ) {
    log.info("workspace changeName operation starts, workspaceId={}, newName={}", workspaceId, newName);
    workspaceAccessChecker.check(RootEntityType.WORKSPACE, workspaceId, memberId);

    Workspace workspace = getWorkspace(workspaceId, "changeName");

    isWorkspaceActive(workspace, "changeName");

    workspace.changeName(newName);
    workspaceRepository.save(workspace);

    log.debug("workspace changeName operation completed, workspaceId={}, newName={}", workspaceId, newName);
    return workspaceMapper.toDto(workspace);
  }

  @Transactional
  @Override
  public WorkspaceDto changeDescription(
          @NotNull UUID workspaceId,
          String newDescription,
          @NotNull UUID memberId
  ) {
    log.info("workspace changeDescription operation starts, workspaceId={}, newDescription={}",
            workspaceId, newDescription);
    workspaceAccessChecker.check(RootEntityType.WORKSPACE, workspaceId, memberId);
    
    Workspace workspace = getWorkspace(workspaceId, "changeDescription");

    isWorkspaceActive(workspace, "changeDescription");

    workspace.changeDescription(newDescription);
    workspaceRepository.save(workspace);

    log.debug("workspace changeDescription operation completed, workspaceId={}, newDescription={}",
            workspaceId, newDescription);
    return workspaceMapper.toDto(workspace);
  }

  // ================ helpers ===================

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
