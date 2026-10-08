package org.example.taskmanager.taskmanager.infrastructure.event;

import jakarta.persistence.OptimisticLockException;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.domain.event.WorkspaceArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.WorkspaceUnarchivedEvent;
import org.example.taskmanager.taskmanager.infrastructure.logs.CustomLogger;
import org.example.taskmanager.taskmanager.service.interfaces.ProjectService;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class WorkspaceEventListener {
  private final ProjectService projectService;

  public WorkspaceEventListener(ProjectService projectService) {
    this.projectService = projectService;
  }

  @Retryable(
          includes = {
                  OptimisticLockException.class,
                  ObjectOptimisticLockingFailureException.class,
                  TransientDataAccessException.class,
                  CannotAcquireLockException.class,
                  PessimisticLockingFailureException.class
          },
          maxRetries = 5,
          delay = 1_000,
          multiplier = 2.0,
          maxDelay = 10_000,
          jitter = 200
  )
  @TransactionalEventListener(
          phase = TransactionPhase.AFTER_COMMIT
  )
  public void handle(WorkspaceArchivedEvent event) {
    UUID id = event.workspaceId();
    CustomLogger.cascadeOperationStarts("archive", "projects", "workspace", id);
    try {
      projectService.archiveByWorkspace(event.workspaceId(), ArchiveReason.PARENT_ARCHIVED);
      CustomLogger.cascadeOperationComplete("archive", "projects", "workspace", id);
    } catch (Exception ex) {
      CustomLogger.cascadeOperationFailed("archive", "projects", "workspace", id);
      throw ex;
    }
  }

  @Retryable(
          includes = {
                  OptimisticLockException.class,
                  ObjectOptimisticLockingFailureException.class,
                  TransientDataAccessException.class,
                  CannotAcquireLockException.class,
                  PessimisticLockingFailureException.class
          },
          maxRetries = 5,
          delay = 1_000,
          multiplier = 2.0,
          maxDelay = 10_000,
          jitter = 200
  )
  @TransactionalEventListener(
          phase = TransactionPhase.AFTER_COMMIT
  )
  public void handle(WorkspaceUnarchivedEvent event) {
    UUID id = event.workspaceId();
    CustomLogger.cascadeOperationStarts("unarchive", "projects", "workspace", id);
    try {
      projectService.unarchiveByWorkspace(event.workspaceId(), UnarchiveReason.CASCADE);
      CustomLogger.cascadeOperationComplete("unarchive", "projects", "workspace", id);
    } catch (Exception ex) {
      CustomLogger.cascadeOperationFailed("unarchive", "projects", "workspace", id);
      throw ex;
    }
  }
}
