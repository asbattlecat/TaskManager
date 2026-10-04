package org.example.taskmanager.taskmanager.infrastructure.event;

import org.example.taskmanager.taskmanager.domain.event.BoardArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.BoardUnarchivedEvent;
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
  public void handle(BoardArchivedEvent event) {

  }

  @EventListener
  public void handle(BoardUnarchivedEvent event) {

  }
}
