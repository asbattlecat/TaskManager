package org.example.taskmanager.taskmanager.repository;

import jakarta.persistence.LockModeType;
import org.example.taskmanager.taskmanager.domain.entity.AuditEntity;
import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
  List<Task> findAllByBoardId(UUID boardId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
    SELECT t 
    FROM task AS t 
    WHERE t.boardId = :boardId
  """)
  List<Task> findTasksByBoardIdWithPessimisticWriteLock(@Param("boardId") UUID boardId);
}
