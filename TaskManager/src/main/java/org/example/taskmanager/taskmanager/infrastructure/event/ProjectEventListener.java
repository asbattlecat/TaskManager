package org.example.taskmanager.taskmanager.infrastructure.event;

import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.domain.event.BoardArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.BoardUnarchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.ProjectArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.ProjectUnarchivedEvent;
import org.example.taskmanager.taskmanager.service.interfaces.BoardService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProjectEventListener {
  private final BoardService boardService;

  public ProjectEventListener(BoardService boardService) {
    this.boardService = boardService;
  }

  @EventListener
  public void handle(ProjectArchivedEvent event) {
    boardService.archiveByProject(event.projectId(), ArchiveReason.PARENT_ARCHIVED);
  }

  @EventListener
  public void handle(ProjectUnarchivedEvent event) {
    boardService.unarchiveByProject(event.projectId(), UnarchiveReason.CASCADE);
  }
}
