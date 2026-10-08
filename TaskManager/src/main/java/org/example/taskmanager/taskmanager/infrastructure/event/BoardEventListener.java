package org.example.taskmanager.taskmanager.infrastructure.event;

import jakarta.persistence.OptimisticLockException;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.domain.event.BoardArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.BoardUnarchivedEvent;
import org.example.taskmanager.taskmanager.infrastructure.logs.CustomLogger;
import org.example.taskmanager.taskmanager.service.interfaces.TaskService;
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
public class BoardEventListener {
  private final TaskService taskService;

  public BoardEventListener(TaskService taskService) {
    this.taskService = taskService;
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
  public void handle(BoardArchivedEvent event) {
    UUID id = event.boardId();
    CustomLogger.cascadeOperationStarts("archive", "tasks", "board", id);
    try {
      taskService.archiveByBoard(event.boardId(), ArchiveReason.PARENT_ARCHIVED);
      CustomLogger.cascadeOperationComplete("archive", "tasks", "board", id);
    } catch (Exception ex) {
      CustomLogger.cascadeOperationFailed("archive", "tasks", "board", id);
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
  public void handle(BoardUnarchivedEvent event) {
    UUID id = event.boardId();
    CustomLogger.cascadeOperationStarts("unarchive", "tasks", "board", id);
    try {
      taskService.unarchiveByBoard(event.boardId(), UnarchiveReason.CASCADE);
      CustomLogger.cascadeOperationComplete("unarchive", "tasks", "board", id);
    } catch (Exception ex) {
      CustomLogger.cascadeOperationFailed("unarchive", "tasks", "board", id);
      throw ex;
    }
  }
}
