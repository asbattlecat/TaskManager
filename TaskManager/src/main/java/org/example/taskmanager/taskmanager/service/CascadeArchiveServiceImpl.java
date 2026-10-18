package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.domain.enums.RootType;
import org.example.taskmanager.taskmanager.domain.records.CascadeArchiveResult;
import org.example.taskmanager.taskmanager.domain.records.CascadeUnarchiveResult;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.TaskRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.CascadeArchiveService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CascadeArchiveServiceImpl implements CascadeArchiveService {
  private final WorkspaceRepository workspaceRepository;
  private final ProjectRepository projectRepository;
  private final BoardRepository boardRepository;
  private final TaskRepository taskRepository;

  public CascadeArchiveServiceImpl(
          WorkspaceRepository workspaceRepository,
          ProjectRepository projectRepository,
          BoardRepository boardRepository,
          TaskRepository taskRepository
  ) {
    this.workspaceRepository = workspaceRepository;
    this.projectRepository = projectRepository;
    this.boardRepository = boardRepository;
    this.taskRepository = taskRepository;
  }

  @Override
  public CascadeArchiveResult archiveByWorkspace(UUID workspaceId) {
    requireWorkspaceExists(workspaceId, "archiveByWorkspace");

    List<UUID> projectIds = projectRepository.archiveByWorkspace(workspaceId);
    List<UUID> boardIds = boardRepository.archiveByWorkspace(workspaceId);
    List<UUID> taskIds = taskRepository.archiveByWorkspace(workspaceId);

    return new CascadeArchiveResult(
            workspaceId,
            RootType.WORKSPACE,
            projectIds.size(),
            boardIds.size(),
            taskIds.size(),
            projectIds,
            boardIds,
            taskIds
    );
  }

  @Override
  public CascadeArchiveResult archiveByProject(UUID projectId) {
    requireProjectExists(projectId, "archiveByProject");

    List<UUID> boardIds = boardRepository.archiveByProject(projectId);
    List<UUID> taskIds = taskRepository.archiveByProject(projectId);

    return new CascadeArchiveResult(
            projectId,
            RootType.PROJECT,
            0,
            boardIds.size(),
            taskIds.size(),
            List.of(),
            boardIds,
            taskIds
    );
  }

  @Override
  public CascadeArchiveResult archiveByBoard(UUID boardId) {
    requireBoardExists(boardId, "archiveByBoard");

    List<UUID> tasksId = taskRepository.archiveByBoard(boardId);

    return new CascadeArchiveResult(
            boardId,
            RootType.BOARD,
            0,
            0,
            tasksId.size(),
            List.of(),
            List.of(),
            tasksId
    );
  }

  @Override
  public CascadeUnarchiveResult unarchiveByWorkspace(UUID workspaceId) {
    requireWorkspaceExists(workspaceId, "unarchiveByWorkspace");

    List<UUID> projectIds = projectRepository.unarchiveByWorkspace(workspaceId);
    List<UUID> boardIds = boardRepository.unarchiveByWorkspace(workspaceId);
    List<UUID> taskIds = taskRepository.unarchiveByWorkspace(workspaceId);

    return new CascadeUnarchiveResult(
            workspaceId,
            RootType.WORKSPACE,
            projectIds.size(),
            boardIds.size(),
            taskIds.size(),
            projectIds,
            boardIds,
            taskIds
    );
  }

  @Override
  public CascadeUnarchiveResult unarchiveByProject(UUID projectId) {
    requireProjectExists(projectId, "unarchiveByProject");

    List<UUID> boardIds = boardRepository.unarchiveByProject(projectId);
    List<UUID> taskIds = taskRepository.unarchiveByProject(projectId);

    return new CascadeUnarchiveResult(
            projectId,
            RootType.PROJECT,
            0,
            boardIds.size(),
            taskIds.size(),
            List.of(),
            boardIds,
            taskIds
    );
  }

  @Override
  public CascadeUnarchiveResult unarchiveByBoard(UUID boardId) {
    requireBoardExists(boardId, "unarchiveByBoard");

    List<UUID> taskIds = taskRepository.unarchiveByBoard(boardId);

    return new CascadeUnarchiveResult(
            boardId,
            RootType.BOARD,
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
