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
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEventPublisher;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.logs.CustomLogger;
import org.example.taskmanager.taskmanager.mapper.BoardMapper;
import org.example.taskmanager.taskmanager.mapper.ProjectMapper;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProjectServiceImpl implements ProjectService {
  private final WorkspaceRepository  workspaceRepository;
  private final ProjectRepository projectRepository;
  private final BoardRepository boardRepository;
  private final ProjectMapper projectMapper;
  private final BoardMapper boardMapper;

  private final DomainEventPublisher domainEventPublisher;

  public ProjectServiceImpl(
          DomainEventPublisher domainEventPublisher,
          WorkspaceRepository workspaceRepository,
          ProjectRepository projectRepository,
          BoardRepository boardRepository,
          ProjectMapper projectMapper,
          BoardMapper boardMapper
  ) {
    this.domainEventPublisher = domainEventPublisher;
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

    log.debug("project created and saved to database, id={}, workspaceId={}, name={}, description={}",
            project.getId(), workspaceId, name, description);
    return projectMapper.toDto(project);
  }

  @Transactional
  @Override
  public ProjectDto archive(UUID projectId, ArchiveReason reason) {
    log.info("project archive starts, id={}, reason={}", projectId, reason);
    Project project = getProject(projectId, "archive");

    project.archive(reason);
    projectRepository.save(project);
    project.getEvents().forEach(domainEventPublisher::publish);;
    project.clearEvents();

    log.debug("project archive completed, id={}, reason={}", projectId, reason);
    return projectMapper.toDto(project);
  }

  @Transactional
  @Override
  public List<ProjectDto> archiveByWorkspace(UUID workspaceId, ArchiveReason reason) {
    log.info("project archiveByWorkspace starts, workspaceId={}, reason={}", workspaceId, reason);

    List<Project> projects = projectRepository
            .findProjectsByWorkspaceIdWithPessimisticWriteLock(workspaceId);

    for (Project project : projects) {
      if (project.getArchiveState() == ArchiveState.ACTIVE) {
        project.archive(reason);
        projectRepository.save(project);
        project.getEvents().forEach(domainEventPublisher::publish);;
        project.clearEvents();
      }
    }

    log.debug("project archiveByWorkspace completed, workspaceId={}, reason={}", workspaceId, reason);
    return projects.stream().map(projectMapper::toDto).toList();
  }

  @Transactional
  @Override
  public ProjectDto unarchive(UUID projectId, UnarchiveReason reason) {
    log.info("project unarchive starts, id={}, reason={}", projectId, reason);

    Project project = getProject(projectId, "unarchive");
    Workspace parent = getParent(project.getWorkspaceId(), "unarchive");

    checkParentBeforeUnarchive(parent);

    project.unarchive(reason);
    projectRepository.save(project);
    project.getEvents().forEach(domainEventPublisher::publish);
    project.clearEvents();

    log.debug("project unarchive completed, id={}, reason={}", projectId, reason);
    return projectMapper.toDto(project);
  }

  @Transactional
  @Override
  public List<ProjectDto> unarchiveByWorkspace(UUID workspaceId, UnarchiveReason reason) {
    log.info("project unarchiveByWorkspace starts, workspaceId={}, reason={}", workspaceId, reason);

    Workspace parent = getParent(workspaceId, "unarchiveByWorkspace");
    List<Project> projects = projectRepository
            .findProjectsByWorkspaceIdWithPessimisticWriteLock(workspaceId);

    checkParentBeforeUnarchive(parent);

    for (Project project : projects) {
      if (project.canBeUnarchived(reason)) {
        project.unarchive(reason);
        projectRepository.save(project);
        project.getEvents().forEach(domainEventPublisher::publish);
        project.clearEvents();
      }
    }

    log.info("project unarchiveByWorkspace completed, workspaceId={}, reason={}", workspaceId, reason);
    return projects.stream().map(projectMapper::toDto).toList();
  }

  @Transactional
  @Override
  public List<BoardDto> getProjectBoards(UUID projectId) {
    CustomLogger.operationStarts("project", "getProjectBoards", "projectId", projectId);
    Project project = getProject(projectId, "getProjectBoards");

    isProjectActive(project, "getProjectBoards");

    List<Board> boards = boardRepository.findAllByProjectId(projectId);

    CustomLogger.operationCompleted("project", "getProjectBoards", "projectId", projectId);
    return boards.stream().map(boardMapper::toDto).toList();
  }

  @Transactional
  @Override
  public ProjectDto changeName(UUID projectId, String newName) {
    CustomLogger.operationStarts("project", "changeName", "projectId", projectId);
    Project project = getProject(projectId, "change name");

    isProjectActive(project, "changeName");

    project.setName(newName);
    projectRepository.save(project);

    CustomLogger.operationCompleted("project", "changeName", "projectId", projectId);
    return projectMapper.toDto(project);
  }

  @Transactional
  @Override
  public ProjectDto changeDescription(UUID projectId, String newDescription) {
    CustomLogger.operationStarts("project", "changeDescription", "projectId", projectId);
    Project project = getProject(projectId, "change description");

    isProjectActive(project, "changeDescription");

    project.setDescription(newDescription);
    projectRepository.save(project);

    CustomLogger.operationCompleted("project", "changeDescription", "projectId", projectId);
    return projectMapper.toDto(project);
  }

  private Project getProject(UUID projectId, String operationName) {
    return projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project with id "
                    + projectId + " not found during "
                    + operationName + "operation"));
  }

  private Workspace getParent(UUID workspaceId,String operationName) {
    return workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace as parent of project with id "
                    + workspaceId + " not found during " + operationName + " operation"));
  }

  private void checkParentBeforeUnarchive(Workspace parent) {
    if (parent.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalStateException("Cannot unarchive project because workspace (parent) with id "
              + parent.getId() + " is archived");
  }

  private void isProjectActive(Project project, String operationName) {
    if (project.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalStateException("Cannot do " + operationName
              + " operation because project with id " + project.getId() + " is archived");
  }
}
