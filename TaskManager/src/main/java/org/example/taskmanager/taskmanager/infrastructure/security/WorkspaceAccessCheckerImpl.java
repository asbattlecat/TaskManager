package org.example.taskmanager.taskmanager.infrastructure.security;

import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.example.taskmanager.taskmanager.domain.entity.WorkspaceMember;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.AccessDeniedException;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.NotFoundException;
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

  @Override
  public void check(Task task, UUID workspaceMemberId) {
    Board board = boardRepository.findById(task.getBoardId())
            .orElseThrow(() -> new NotFoundException("Board not found"));
    Project project = projectRepository.findById(board.getProjectId())
            .orElseThrow(() -> new NotFoundException("Project not found"));

    WorkspaceMember member = workspaceMemberRepository.findById(workspaceMemberId)
            .orElseThrow(() -> new NotFoundException("Workspace member not found"));

    if (!member.getWorkspaceId().equals(project.getWorkspaceId())) {
      throw new AccessDeniedException("This workspace member is not part of this workspace");
    }
  }
}
