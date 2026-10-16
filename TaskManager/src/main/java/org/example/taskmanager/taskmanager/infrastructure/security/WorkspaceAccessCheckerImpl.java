package org.example.taskmanager.taskmanager.infrastructure.security;

import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.domain.entity.WorkspaceMember;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceMemberState;
import org.example.taskmanager.taskmanager.infrastructure.exception.AccessDeniedException;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceMemberRepository;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceAccessChecker;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
   * @param boardId ID of Board (UUID)
   * @param workspaceMemberId ID of WorkspaceMember (UUID)
   */
  @Override
  public void check(UUID boardId, UUID workspaceMemberId) {
    UUID workspaceId = getWorkspaceIdByBoard(boardId);
    WorkspaceMember member = getMember(workspaceMemberId);
    requireMemberActive(member);
    requireMemberPartOfWorkspace(member, workspaceId);
  }

  /**
   * Проверка прав доступа двоих указанных WorkspaceMember к Workspace по boardId,
   * проверка пользователя на статус (должен быть активен). Этот метод был
   * добавлен во благо уменьшения количества запросов к бд.
   * @param boardId ID of Board (UUID)
   * @param firstId ID of first WorkspaceMember (UUID)
   * @param secondId ID of second WorkspaceMember (uUID)
   */
  @Override
  public void checkTwo(UUID boardId, UUID firstId, UUID secondId) {
    UUID workspaceId = getWorkspaceIdByBoard(boardId);
    WorkspaceMember first = getMember(firstId);
    WorkspaceMember second = getMember(secondId);
    requireMemberActive(first);
    requireMemberActive(second);
    requireMemberPartOfWorkspace(first, workspaceId);
    requireMemberPartOfWorkspace(second, workspaceId);
  }

  // ========== helpers ===============

  private UUID getWorkspaceIdByBoard(UUID boardId) {
    Board board = getBoard(boardId);
    if (board.getArchiveState() != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Board with id=" + board.getId() + " is archived");
    }
    Project project = getProject(board.getProjectId());
    if (project.getArchiveState() != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Project with id=" + project.getId() + " is archived");
    }
    return project.getWorkspaceId();
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

  private Board getBoard(UUID boardId) {
    return boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundException("Board not found, id=" + boardId));
  }

  private Project getProject(UUID projectId) {
    return projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found, id=" + projectId));
  }


  private WorkspaceMember getMember(UUID workspaceMemberId) {
    return workspaceMemberRepository.findById(workspaceMemberId)
            .orElseThrow(() ->
                    new NotFoundException("Workspace member not found, id=" + workspaceMemberId));
  }
}
