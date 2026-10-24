package org.example.taskmanager.taskmanager.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.aggregate.Board;
import org.example.taskmanager.taskmanager.domain.aggregate.Project;
import org.example.taskmanager.taskmanager.domain.aggregate.Workspace;
import org.example.taskmanager.taskmanager.domain.aggregate.WorkspaceMember;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.RootEntityType;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceMemberState;
import org.example.taskmanager.taskmanager.infrastructure.exception.AccessDeniedException;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceMemberRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceAccessChecker;
import org.springframework.stereotype.Service;

import java.util.UUID;


// TODO: может, при любом исключительном случае кидать AccessDeniedException?
@Slf4j
@Service
public class WorkspaceAccessCheckerImpl implements WorkspaceAccessChecker {
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final WorkspaceRepository workspaceRepository;
  private final ProjectRepository projectRepository;
  private final BoardRepository boardRepository;

  public  WorkspaceAccessCheckerImpl(
          WorkspaceMemberRepository workspaceMemberRepository,
          WorkspaceRepository workspaceRepository,
          ProjectRepository projectRepository,
          BoardRepository boardRepository
  ) {
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.workspaceRepository = workspaceRepository;
    this.projectRepository = projectRepository;
    this.boardRepository = boardRepository;
  }

  /**
   * Проверка прав доступа указанного WorkspaceMember к Workspace по boardId,
   * проверка пользователя на статус (должен быть активен)
   * @param rootEntityId ID of RootEntity (UUID)
   * @param workspaceMemberId ID of WorkspaceMember (UUID)
   */
  @Override
  public void check(
          RootEntityType rootEntityType,
          UUID rootEntityId,
          UUID workspaceMemberId
  ) {
    try {
      doCheck(rootEntityType, rootEntityId, workspaceMemberId);
    } catch (NotFoundException | IllegalStateException e) {
      log.warn("Access denied: memberId={}, rootEntityType={}, rootEntityId={}, reason={}",
              workspaceMemberId, rootEntityType, rootEntityId, e.getMessage(), e);
      throw new AccessDeniedException("Access denied", e);
    }
  }

  /**
   * Проверка прав доступа двоих указанных WorkspaceMember к Workspace по boardId,
   * проверка пользователя на статус (должен быть активен). Этот метод был
   * добавлен во благо уменьшения количества запросов к бд.
   * @param rootEntityId ID of RootEntity (UUID)
   * @param firstMemberId ID of first WorkspaceMember (UUID)
   * @param secondMemberId ID of second WorkspaceMember (uUID)
   */
  @Override
  public void checkTwo(
          RootEntityType rootEntityType,
          UUID rootEntityId,
          UUID firstMemberId,
          UUID secondMemberId
  ) {
    try {
      doCheckTwo(rootEntityType, rootEntityId, firstMemberId, secondMemberId);
    } catch (NotFoundException | IllegalStateException e) {
      log.warn("Access denied: firstMemberId={}, secondMemberId={}, rootEntityType={}, rootEntityId={}, reason={}",
              firstMemberId, secondMemberId, rootEntityType, rootEntityId, e.getMessage(), e);
    }
  }

  // TODO не повредит ли проверка на активность внутри этих методов вызываемым методам?
  // TODO не повредит ли проверка на активность внутри этих методов вызываемым методам?
  // TODO не повредит ли проверка на активность внутри этих методов вызываемым методам?
  // к примеру, методу unarchive в сервисах


  // ========== helpers ===============

  private void doCheck(
          RootEntityType rootEntityType,
          UUID rootEntityId,
          UUID workspaceMemberId
  ) {
    UUID workspaceId = getWorkspaceId(rootEntityId, rootEntityType);
    WorkspaceMember member = getMember(workspaceMemberId);
    requireMemberActive(member);
    requireMemberPartOfWorkspace(member, workspaceId);
  }

  private void doCheckTwo(
          RootEntityType rootEntityType,
          UUID rootEntityId,
          UUID firstMemberId,
          UUID secondMemberId
  ) {
    UUID workspaceId = getWorkspaceId(rootEntityId, rootEntityType);
    WorkspaceMember first = getMember(firstMemberId);
    requireMemberActive(first);
    requireMemberPartOfWorkspace(first, workspaceId);
    WorkspaceMember second = getMember(secondMemberId);
    requireMemberActive(second);
    requireMemberPartOfWorkspace(second, workspaceId);
  }

  private UUID getWorkspaceId(UUID rootEntityId, RootEntityType rootEntityType) {
    UUID workspaceId;
    if (rootEntityType == RootEntityType.BOARD) {
      workspaceId = getWorkspaceIdByBoard(rootEntityId);
    } else if (rootEntityType == RootEntityType.PROJECT) {
      workspaceId = getWorkspaceIdByProject(rootEntityId);
    } else {
      workspaceId = rootEntityId;
    }
    requireWorkspaceActive(workspaceId);
    return workspaceId;
  }

  private Board getBoard(UUID boardId) {
    Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundException("Board not found, id=" + boardId));
    requireBoardActive(board);
    return board;
  }

  private Project getProject(UUID projectId) {
    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found, id=" + projectId));
    requireProjectActive(project);

    return project;
  }

  private UUID getWorkspaceIdByBoard(UUID boardId) {
    Board board = getBoard(boardId);
    Project project = getProject(board.getProjectId());
    return project.getWorkspaceId();
  }

  private UUID getWorkspaceIdByProject(UUID projectId) {
    Project project = getProject(projectId);
    return project.getWorkspaceId();
  }

  private void requireWorkspaceActive(UUID workspaceId) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found, id=" + workspaceId));

    if (workspace.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalStateException("Workspace with id=" + workspaceId + " is archived");
  }

  private void requireBoardActive(Board board) {
    if (board.getArchiveState() != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Board with id=" + board.getId() + " is archived");
    }
  }

  private void requireProjectActive(Project project) {
    if (project.getArchiveState() != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Project with id=" + project.getId() + " is archived");
    }
  }

  private void requireMemberActive(WorkspaceMember member) {
    if (member.getState() != WorkspaceMemberState.ACTIVE) {
      throw new IllegalStateException("WorkspaceMember with id=" + member.getId() + " is archived");
    }
  }

  private void requireMemberPartOfWorkspace(WorkspaceMember member, UUID workspaceId) {
    if (!member.getWorkspaceId().equals(workspaceId)) {
      throw new AccessDeniedException("Workspace member is not part of this workspace");
    }
  }


  private WorkspaceMember getMember(UUID workspaceMemberId) {
    return workspaceMemberRepository.findById(workspaceMemberId)
            .orElseThrow(() ->
                    new NotFoundException("Workspace member not found, id=" + workspaceMemberId));
  }
}
