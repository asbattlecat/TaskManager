package org.example.taskmanager.taskmanager.infrastructure.event;

import org.example.taskmanager.taskmanager.domain.event.WorkspaceArchivedEvent;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceEventListener {
  private final ProjectRepository projectRepository;

  public WorkspaceEventListener(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  @EventListener
  public void handle(WorkspaceArchivedEvent event) {

  }

}
