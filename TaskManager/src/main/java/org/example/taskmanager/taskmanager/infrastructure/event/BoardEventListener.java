package org.example.taskmanager.taskmanager.infrastructure.event;

import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.domain.event.BoardArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.BoardUnarchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.TaskArchivedEvent;
import org.example.taskmanager.taskmanager.repository.TaskRepository;
import org.example.taskmanager.taskmanager.service.interfaces.TaskService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BoardEventListener {
  private final TaskService taskService;

  public BoardEventListener(TaskService taskService) {
    this.taskService = taskService;
  }

  @EventListener
  public void handle(BoardArchivedEvent event) {
    taskService.archiveByBoard(event.boardId(), ArchiveReason.PARENT_ARCHIVED);
  }

  @EventListener
  public void handle(BoardUnarchivedEvent event) {
    taskService.unarchiveByBoard(event.boardId(), UnarchiveReason.CASCADE);
  }
}
