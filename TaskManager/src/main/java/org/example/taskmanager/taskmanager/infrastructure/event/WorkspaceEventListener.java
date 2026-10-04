package org.example.taskmanager.taskmanager.infrastructure.event;

import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.domain.event.WorkspaceArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.WorkspaceUnarchivedEvent;
import org.example.taskmanager.taskmanager.service.interfaces.ProjectService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceEventListener {
  private final ProjectService projectService;

  public WorkspaceEventListener(ProjectService projectService) {
    this.projectService = projectService;
  }

  @EventListener
  public void handle(WorkspaceArchivedEvent event) {
    projectService.archiveByWorkspace(event.workspaceId(), ArchiveReason.PARENT_ARCHIVED);
  }

  @EventListener
  public void handle(WorkspaceUnarchivedEvent event) {
    projectService.unarchiveByWorkspace(event.workspaceId(), UnarchiveReason.CASCADE);
  }
}
