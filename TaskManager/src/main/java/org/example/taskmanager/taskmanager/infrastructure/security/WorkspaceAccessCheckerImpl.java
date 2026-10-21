package org.example.taskmanager.taskmanager.infrastructure.security;

import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.domain.entity.WorkspaceMember;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.RootEntityType;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceMemberState;
import org.example.taskmanager.taskmanager.infrastructure.exception.AccessDeniedException;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceMemberRepository;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceAccessChecker;
import org.springframework.stereotype.Service;

import java.util.UUID;


// TODO: может, при любом исключительном случае кидать AccessDeniedException?
@Service
public class WorkspaceAccessCheckerImpl implements WorkspaceAccessChecker {
  private final BoardRepository boardRepository;
  private final ProjectRepository projectRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  public  WorkspaceAccessCheckerImpl(BoardRepository boardRepository,
                                     ProjectRepository projectRepository,
                                     WorkspaceMemberRepository workspaceMemberRepository) {
    this.boardRepository = boardRepository;
    this.projectRepository = projectRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
  }

  /**
   * Проверка прав доступа указанного WorkspaceMember к Workspace по boardId,
   * проверка пользователя на статус (должен быть активен)
   * @param rootEntityId ID of RootEntity (UUID)
   * @param workspaceMemberId ID of WorkspaceMember (UUID)
   */
  @Override
  public void check(RootEntityType rootEntityType, UUID rootEntityId, UUID workspaceMemberId) {
    UUID workspaceId = getWorkspaceId(rootEntityId, rootEntityType);
    WorkspaceMember member = getMember(workspaceMemberId);
    requireMemberActive(member);
    requireMemberPartOfWorkspace(member, workspaceId);
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
  public void checkTwo(RootEntityType rootEntityType, UUID rootEntityId, UUID firstMemberId, UUID secondMemberId) {
    UUID workspaceId = getWorkspaceId(rootEntityId, rootEntityType);
    WorkspaceMember first = getMember(firstMemberId);
    WorkspaceMember second = getMember(secondMemberId);
    requireMemberActive(first);
    requireMemberActive(second);
    requireMemberPartOfWorkspace(first, workspaceId);
    requireMemberPartOfWorkspace(second, workspaceId);
  }

  // ========== helpers ===============

  private UUID getWorkspaceId(UUID rootEntityId, RootEntityType rootEntityType) {
    UUID workspaceId = null;
    if (rootEntityType == RootEntityType.BOARD) {
      workspaceId = getWorkspaceIdByBoard(rootEntityId);
    } else if (rootEntityType == RootEntityType.PROJECT) {
      workspaceId = getWorkspaceIdByProject(rootEntityId);
    } else if (rootEntityType == RootEntityType.WORKSPACE) {
      workspaceId = rootEntityId;
    }
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
      throw new IllegalStateException("Member is not active, cannot do operations, memberId="
              + member.getId() + ", memberState=" + member.getState());
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
