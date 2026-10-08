package org.example.taskmanager.taskmanager.repository;

import jakarta.persistence.LockModeType;
import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
    SELECT b 
    FROM board AS b 
    WHERE b.projectId = :projectId
  """)
  List<Board> findBoardsByProjectIdWithPessimisticWriteLock(@Param("projectId") UUID projectId);

  List<Board> findAllByProjectId(UUID projectId);
}
