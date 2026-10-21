package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.domain.enums.RootEntityType;
import org.example.taskmanager.taskmanager.domain.records.CascadeArchiveResult;
import org.example.taskmanager.taskmanager.domain.records.CascadeUnarchiveResult;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.TaskRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.AuditEntityService;
import org.example.taskmanager.taskmanager.service.interfaces.CascadeArchiveService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CascadeArchiveServiceImpl implements CascadeArchiveService {
  private final WorkspaceRepository workspaceRepository;
  private final AuditEntityService auditEntityService;
  private final ProjectRepository projectRepository;
  private final BoardRepository boardRepository;
  private final TaskRepository taskRepository;

  public CascadeArchiveServiceImpl(
          WorkspaceRepository workspaceRepository,
          AuditEntityService auditEntityService,
          ProjectRepository projectRepository,
          BoardRepository boardRepository,
          TaskRepository taskRepository
  ) {
    this.workspaceRepository = workspaceRepository;
    this.auditEntityService = auditEntityService;
    this.projectRepository = projectRepository;
    this.boardRepository = boardRepository;
    this.taskRepository = taskRepository;
  }

  @Transactional
  @Override
  public CascadeArchiveResult archiveByWorkspace(UUID workspaceId, UUID memberId) {
    requireWorkspaceExists(workspaceId, "archiveByWorkspace");

    List<UUID> projectIds = projectRepository.archiveByWorkspace(workspaceId);
    List<UUID> boardIds = boardRepository.archiveByWorkspace(workspaceId);
    List<UUID> taskIds = taskRepository.archiveByWorkspace(workspaceId);

    for (UUID id : taskIds) {
      auditEntityService.create(
              id,
              memberId,
              "archiveState",
              "ACTIVE",
              "PARENT_ARCHIVED"
      );
    }

    return new CascadeArchiveResult(
            workspaceId,
            RootEntityType.WORKSPACE,
            projectIds.size(),
            boardIds.size(),
            taskIds.size(),
            projectIds,
            boardIds,
            taskIds
    );
  }

  @Transactional
  @Override
  public CascadeArchiveResult archiveByProject(UUID projectId, UUID memberId) {
    requireProjectExists(projectId, "archiveByProject");

    List<UUID> boardIds = boardRepository.archiveByProject(projectId);
    List<UUID> taskIds = taskRepository.archiveByProject(projectId);

    for (UUID id : taskIds) {
      auditEntityService.create(
              id,
              memberId,
              "archiveState",
              "ACTIVE",
              "PARENT_ARCHIVED"
      );
    }

    return new CascadeArchiveResult(
            projectId,
            RootEntityType.PROJECT,
            0,
            boardIds.size(),
            taskIds.size(),
            List.of(),
            boardIds,
            taskIds
    );
  }

  @Transactional
  @Override
  public CascadeArchiveResult archiveByBoard(UUID boardId, UUID memberId) {
    requireBoardExists(boardId, "archiveByBoard");

    List<UUID> taskIds = taskRepository.archiveByBoard(boardId);

    for (UUID id : taskIds) {
      auditEntityService.create(
              id,
              memberId,
              "archiveState",
              "ACTIVE",
              "PARENT_ARCHIVED"
      );
    }

    return new CascadeArchiveResult(
            boardId,
            RootEntityType.BOARD,
            0,
            0,
            taskIds.size(),
            List.of(),
            List.of(),
            taskIds
    );
  }

  @Transactional
  @Override
  public CascadeUnarchiveResult unarchiveByWorkspace(UUID workspaceId, UUID memberId) {
    requireWorkspaceExists(workspaceId, "unarchiveByWorkspace");

    List<UUID> projectIds = projectRepository.unarchiveByWorkspace(workspaceId);
    List<UUID> boardIds = boardRepository.unarchiveByWorkspace(workspaceId);
    List<UUID> taskIds = taskRepository.unarchiveByWorkspace(workspaceId);

    for (UUID id : taskIds) {
      auditEntityService.create(
              id,
              memberId,
              "archiveState",
              "PARENT_ARCHIVED",
              "ACTIVE"
      );
    }

    return new CascadeUnarchiveResult(
            workspaceId,
            RootEntityType.WORKSPACE,
            projectIds.size(),
            boardIds.size(),
            taskIds.size(),
            projectIds,
            boardIds,
            taskIds
    );
  }

  @Transactional
  @Override
  public CascadeUnarchiveResult unarchiveByProject(UUID projectId, UUID memberId) {
    requireProjectExists(projectId, "unarchiveByProject");

    List<UUID> boardIds = boardRepository.unarchiveByProject(projectId);
    List<UUID> taskIds = taskRepository.unarchiveByProject(projectId);

    for (UUID id : taskIds) {
      auditEntityService.create(
              id,
              memberId,
              "archiveState",
              "PARENT_ARCHIVED",
              "ACTIVE"
      );
    }

    return new CascadeUnarchiveResult(
            projectId,
            RootEntityType.PROJECT,
            0,
            boardIds.size(),
            taskIds.size(),
            List.of(),
            boardIds,
            taskIds
    );
  }

  @Transactional
  @Override
  public CascadeUnarchiveResult unarchiveByBoard(UUID boardId, UUID memberId) {
    requireBoardExists(boardId, "unarchiveByBoard");

    List<UUID> taskIds = taskRepository.unarchiveByBoard(boardId);

    for (UUID id : taskIds) {
      auditEntityService.create(
              id,
              memberId,
              "archiveState",
              "PARENT_ARCHIVED",
              "ACTIVE"
      );
    }

    return new CascadeUnarchiveResult(
            boardId,
            RootEntityType.BOARD,
            0,
            0,
            taskIds.size(),
            List.of(),
            List.of(),
            taskIds
    );
  }

  // =========== helpers ==============

  private void requireWorkspaceExists(UUID workspaceId, String operationName) {
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new NotFoundException("Workspace with id=" + workspaceId
              + " not found, cannot " + operationName);
    }
  }

  private void requireProjectExists(UUID projectId, String operationName) {
    if (!projectRepository.existsById(projectId)) {
      throw new NotFoundException("Project with id=" + projectId
              + " not found, cannot " + operationName);
    }
  }

  private void requireBoardExists(UUID boardId, String operationName) {
    if (!boardRepository.existsById(boardId)) {
      throw new NotFoundException("Board with id=" + boardId
              + " not found, cannot " + operationName);
    }
  }
}
