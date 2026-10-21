package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.response.ProjectDto;
import org.example.taskmanager.taskmanager.domain.entity.Workspace;
import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.domain.records.CascadeArchiveResult;
import org.example.taskmanager.taskmanager.domain.records.CascadeUnarchiveResult;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEventPublisher;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.BoardMapper;
import org.example.taskmanager.taskmanager.mapper.ProjectMapper;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.CascadeArchiveService;
import org.example.taskmanager.taskmanager.service.interfaces.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProjectServiceImpl implements ProjectService {
  private final CascadeArchiveService cascadeArchiveService;
  private final WorkspaceRepository  workspaceRepository;
  private final ProjectRepository projectRepository;
  private final BoardRepository boardRepository;
  private final ProjectMapper projectMapper;
  private final BoardMapper boardMapper;

  public ProjectServiceImpl(
          CascadeArchiveService cascadeArchiveService,
          WorkspaceRepository workspaceRepository,
          ProjectRepository projectRepository,
          BoardRepository boardRepository,
          ProjectMapper projectMapper,
          BoardMapper boardMapper
  ) {
    this.cascadeArchiveService = cascadeArchiveService;
    this.workspaceRepository = workspaceRepository;
    this.projectRepository = projectRepository;
    this.boardRepository = boardRepository;
    this.projectMapper = projectMapper;
    this.boardMapper = boardMapper;
  }

  @Transactional
  @Override
  public ProjectDto create(UUID workspaceId, String name, String description) {
    log.info("creating project, workspaceId={}, name={}, description={}", workspaceId, name, description);

    if (!workspaceRepository.existsById(workspaceId)) {
      log.error("project was not created, workspace not found, workspaceId={}", workspaceId);
      throw new NotFoundException("Workspace with id " + workspaceId + " not found during Project create operation");
    }

    Project project = new Project(workspaceId, name, description);
    projectRepository.save(project);

    log.debug("project created and saved in database, projectId={}", project.getId());
    return projectMapper.toDto(project);
  }

  // ========================== архивация/разархивация =====================

  // TODO валидация пользователя
  @Transactional
  @Override
  public ProjectDto archive(UUID projectId, UUID memberId) {
    log.info("project archive starts, projectId={}", projectId);
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

  // TODO валидация пользователя
  @Transactional
  @Override
  public ProjectDto unarchive(UUID projectId, UUID memberId) {
    log.info("project unarchive starts, projectId={}", projectId);

    Project project = getProject(projectId, "unarchive");
    Workspace parent = getParent(project.getWorkspaceId(), "unarchive");

    checkParentBeforeUnarchive(parent);

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
  public List<BoardDto> getProjectBoards(UUID projectId) {
    log.info("project getProjectBoards operation starts, projectId={}", projectId);
    Project project = getProject(projectId, "getProjectBoards");

    isProjectActive(project, "getProjectBoards");

    List<Board> boards = boardRepository.findAllByProjectId(projectId);

    log.debug("board getProjectBoards operation completed, projectId={}", projectId);
    return boards.stream().map(boardMapper::toDto).toList();
  }

  @Transactional
  @Override
  public ProjectDto changeName(UUID projectId, String newName) {
    log.info("project changeName operation starts, projectId={}, newName={}", projectId, newName);
    Project project = getProject(projectId, "changeName");

    isProjectActive(project, "changeName");

    project.setName(newName);
    projectRepository.save(project);

    log.debug("project changeName operation completed, projectId={}, newName={}", projectId, newName);
    return projectMapper.toDto(project);
  }

  @Transactional
  @Override
  public ProjectDto changeDescription(UUID projectId, String newDescription) {
    log.info("project changeDescription operation starts, projectId={}, newDescription={}", projectId, newDescription);
    Project project = getProject(projectId, "changeDescription");

    isProjectActive(project, "changeDescription");

    project.setDescription(newDescription);
    projectRepository.save(project);

    log.info("project changeDescription operation completed, projectId={}, newDescription={}", projectId, newDescription);
    return projectMapper.toDto(project);
  }

  // ================ helpers =================

  private Project getProject(UUID projectId, String operationName) {
    return projectRepository
            .findById(projectId)
            .orElseThrow(() -> {
              log.error("project not found during {}, projectId={}", operationName, projectId);
              return new NotFoundException("Project with id " + projectId
                      + " not found during " + operationName + "operation");
            });
  }

  private Workspace getParent(UUID workspaceId,String operationName) {
    return workspaceRepository
            .findById(workspaceId)
            .orElseThrow(() -> {
              log.error("workspace not found during {}, workspaceId={}", operationName, workspaceId);
              return new NotFoundException("Workspace as parent of project with id "
                      + workspaceId + " not found during " + operationName + " operation");
            });
  }

  private void checkParentBeforeUnarchive(Workspace parent) {
    if (parent.getArchiveState() != ArchiveState.ACTIVE) {
      log.error("cannot unarchive project, workspace (parent) is archived, worksapaceId={}", parent.getId());
      throw new IllegalStateException("Cannot unarchive project because workspace (parent) with id "
              + parent.getId() + " is archived");
    }
  }

  private void isProjectActive(Project project, String operationName) {
    if (project.getArchiveState() != ArchiveState.ACTIVE) {
      log.error("cannot do {}, project is archived, projectId={}", operationName, project.getId());
      throw new IllegalStateException("Cannot do " + operationName
              + " operation because project with id " + project.getId() + " is archived");
    }
  }
}
