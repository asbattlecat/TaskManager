package org.example.taskmanager.taskmanager.infrastructure.event;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.event.UserActivatedEvent;
import org.example.taskmanager.taskmanager.domain.event.UserDeactivatedEvent;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceMemberService;
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
public class UserEventListener {
  private final WorkspaceMemberService workspaceMemberService;

  public UserEventListener(WorkspaceMemberService workspaceMemberService) {
    this.workspaceMemberService = workspaceMemberService;
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
  public void handle(UserDeactivatedEvent event) {
    UUID id = event.userId();
    log.info("cascade workspace member archiveByUser operation starts, userId={}", id);
    try {
      workspaceMemberService.archiveByUser(id);
      log.debug("cascade workspace member archiveByUser operation completed, userId={}", id);
    } catch (Exception ex) {
      log.error("cascade workspace member archiveByUser operation failed, userId={}", id);
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
  public void handle(UserActivatedEvent event) {
    UUID id = event.userId();
    log.info("cascade workspace member unarchiveByUser operation starts, userId={}", id);
    try {
      workspaceMemberService.unarchiveByUser(id);
      log.debug("cascade workspace member unarchiveByUser operation completed, userId={}", id);
    } catch (Exception ex) {
      log.error("cascade workspace member unarchiveByUser operation failed, userId={}", id);
      throw ex;
    }
  }
}
