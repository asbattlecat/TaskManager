package org.example.taskmanager.taskmanager.infrastructure.event;

import jakarta.persistence.OptimisticLockException;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.domain.event.ProjectArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.ProjectUnarchivedEvent;
import org.example.taskmanager.taskmanager.infrastructure.logs.CustomLogger;
import org.example.taskmanager.taskmanager.service.interfaces.BoardService;
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
public class ProjectEventListener {
  private final BoardService boardService;

  public ProjectEventListener(BoardService boardService) {
    this.boardService = boardService;
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
  public void handle(ProjectArchivedEvent event) {
    UUID id = event.projectId();
    CustomLogger.cascadeOperationStarts("archive", "boards", "project", id);
    try {
      boardService.archiveByProject(event.projectId(), ArchiveReason.PARENT_ARCHIVED);
      CustomLogger.cascadeOperationComplete("archive", "boards", "project", id);
    } catch (Exception ex) {
      CustomLogger.cascadeOperationFailed("archive", "boards", "project", id);
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
  public void handle(ProjectUnarchivedEvent event) {
    UUID id = event.projectId();
    CustomLogger.cascadeOperationStarts("unarchive", "boards", "project", id);
    try {
      boardService.unarchiveByProject(event.projectId(), UnarchiveReason.CASCADE);
      CustomLogger.cascadeOperationComplete("archive", "boards", "project", id);
    } catch (Exception ex) {
      CustomLogger.cascadeOperationFailed("archive", "boards", "project", id);
      throw ex;
    }
  }
}
