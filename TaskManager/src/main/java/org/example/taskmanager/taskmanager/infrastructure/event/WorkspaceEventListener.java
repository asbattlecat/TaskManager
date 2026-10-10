package org.example.taskmanager.taskmanager.infrastructure.event;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.domain.event.WorkspaceArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.WorkspaceUnarchivedEvent;
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

@Slf4j
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
    log.info("cascade project archiveByWorkspace operation starts, workspaceId={}", id);
    try {
      projectService.archiveByWorkspace(id, ArchiveReason.PARENT_ARCHIVED);
      log.debug("cascade project archiveByWorkspace operation completed, workspaceId={}", id);
    } catch (Exception ex) {
      log.error("cascade project archiveByWorkspace operation failed, workspaceId={}", id);
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
    log.info("cascade project unarchiveByWorkspace operation starts, workspaceId={}", id);
    try {
      projectService.unarchiveByWorkspace(id, UnarchiveReason.CASCADE);
      log.debug("cascade project unarchiveByWorkspace operation completed, workspaceId={}", id);
    } catch (Exception ex) {
      log.error("cascade project unarchiveByWorkspace operation failed, workspaceId={}", id);
      throw ex;
    }
  }
}
