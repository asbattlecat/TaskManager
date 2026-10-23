package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.response.ProjectDto;
import org.example.taskmanager.taskmanager.domain.aggregate.Workspace;
import org.example.taskmanager.taskmanager.domain.aggregate.Board;
import org.example.taskmanager.taskmanager.domain.aggregate.Project;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.RootEntityType;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.domain.port.WorkspaceAccessPort;
import org.example.taskmanager.taskmanager.domain.records.CascadeArchiveResult;
import org.example.taskmanager.taskmanager.domain.records.CascadeUnarchiveResult;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.BoardMapper;
import org.example.taskmanager.taskmanager.mapper.ProjectMapper;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.service.interfaces.CascadeArchiveService;
import org.example.taskmanager.taskmanager.service.interfaces.ProjectService;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceAccessChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProjectServiceImpl implements ProjectService {
  private final WorkspaceAccessChecker workspaceAccessChecker;
  private final CascadeArchiveService cascadeArchiveService;
  private final WorkspaceAccessPort workspaceAccessPort;
  private final ProjectRepository projectRepository;
  private final BoardRepository boardRepository;
  private final ProjectMapper projectMapper;
  private final BoardMapper boardMapper;

  public ProjectServiceImpl(
          WorkspaceAccessChecker workspaceAccessChecker,
          CascadeArchiveService cascadeArchiveService,
          WorkspaceAccessPort workspaceAccessPort,
          ProjectRepository projectRepository,
          BoardRepository boardRepository,
          ProjectMapper projectMapper,
          BoardMapper boardMapper
  ) {
    this.workspaceAccessChecker = workspaceAccessChecker;
    this.cascadeArchiveService = cascadeArchiveService;
    this.workspaceAccessPort = workspaceAccessPort;
    this.projectRepository = projectRepository;
    this.boardRepository = boardRepository;
    this.projectMapper = projectMapper;
    this.boardMapper = boardMapper;
  }

  @Transactional
  @Override
  public ProjectDto create(
          UUID workspaceId,
          String name,
          String description,
          UUID memberId
  ) {
    log.info("creating project, workspaceId={}, name={}, description={}", workspaceId, name, description);
    workspaceAccessChecker.check(RootEntityType.WORKSPACE, workspaceId, memberId);

    if (!workspaceAccessPort.exists(workspaceId)) {
      throw new NotFoundException("Workspace with id " + workspaceId
              + " not found during Project create operation");
    }

    Project project = new Project(workspaceId, name, description);
    projectRepository.save(project);

    log.debug("project created and saved in database, projectId={}", project.getId());
    return projectMapper.toDto(project);
  }

  // ========================== архивация/разархивация =====================

  @Transactional
  @Override
  public ProjectDto archive(
          UUID projectId,
          UUID memberId
  ) {
    log.info("project archive starts, projectId={}", projectId);
    workspaceAccessChecker.check(RootEntityType.PROJECT, projectId, memberId);
    Project project = getProject(projectId, "archive");

    project.archive(ArchiveReason.USER_ACTION);
    projectRepository.save(project);
    CascadeArchiveResult result = cascadeArchiveService.archiveByProject(projectId, memberId);

    log.debug("project archive completed, projectId={}, cascaded projects={}, boards={}, tasks={}",
            projectId,
            result.projectsArchived(),
            result.boardsArchived(),
            result.tasksArchived()
    );
    return projectMapper.toDto(project);
  }

  @Transactional
  @Override
  public ProjectDto unarchive(
          UUID projectId,
          UUID memberId
  ) {
    log.info("project unarchive starts, projectId={}", projectId);
    // проверка активности родителя происходит внутри check
    workspaceAccessChecker.check(RootEntityType.PROJECT, projectId, memberId);

    Project project = getProject(projectId, "unarchive");

    project.unarchive(UnarchiveReason.USER_ACTION);
    projectRepository.save(project);
    CascadeUnarchiveResult result = cascadeArchiveService.unarchiveByProject(projectId, memberId);

    log.debug("project unarchive completed, projectId={}, cascaded projects={}, boards={}, tasks={}",
            projectId,
            result.projectsUnarchived(),
            result.projectsUnarchived(),
            result.projectsUnarchived()
    );
    return projectMapper.toDto(project);
  }

  // ========================== архивация/разархивация =====================

  @Transactional(readOnly = true)
  @Override
  public List<BoardDto> getProjectBoards(
          UUID projectId,
          UUID memberId
  ) {
    Project project = getProject(projectId, "getProjectBoards");
    requireProjectActive(project, "getProjectBoards");

    List<Board> boards = boardRepository.findAllByProjectId(projectId);

    log.debug("ProjectService getProjectBoards  completed, projectId={}", projectId);
    return boards.stream().map(boardMapper::toDto).toList();
  }

  @Transactional
  @Override
  public ProjectDto changeName(
          UUID projectId,
          String newName,
          UUID memberId
  ) {
    Project project = getProject(projectId, "changeName");

    // проверка на активность происходит внутри агрегата
    project.changeName(newName);
    projectRepository.save(project);

    log.debug("ProjectService changeName completed, projectId={}, newName={}", projectId, newName);
    return projectMapper.toDto(project);
  }

  @Transactional
  @Override
  public ProjectDto changeDescription(
          UUID projectId,
          String newDescription,
          UUID memberId
  ) {
    Project project = getProject(projectId, "changeDescription");

    // проверка на активность происходит внутри агрегата
    project.changeDescription(newDescription);
    projectRepository.save(project);

    log.debug("ProjectService changeDescription completed, projectId={}, newDescription={}", projectId, newDescription);
    return projectMapper.toDto(project);
  }

  // ================ helpers =================

  private Project getProject(UUID projectId, String operationName) {
    return projectRepository
            .findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project with id " + projectId
                      + " not found during " + operationName + "operation"));
  }

  private Workspace getParent(UUID workspaceId,String operationName) {
    return workspaceRepository
            .findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace as parent of project with id "
                      + workspaceId + " not found during " + operationName + " operation"));
  }

  private void checkParentBeforeUnarchive(Workspace parent) {
    if (parent.getArchiveState() != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Cannot unarchive project because workspace (parent) with id "
              + parent.getId() + " is archived");
    }
  }

  private void requireProjectActive(Project project, String operationName) {
    if (project.getArchiveState() != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Cannot do " + operationName
              + ", project is archived, id=" + project.getId());
    }
  }
}
