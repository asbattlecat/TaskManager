package org.example.taskmanager.taskmanager.repository;

import jakarta.persistence.LockModeType;
import org.example.taskmanager.taskmanager.domain.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM BoardColumn c WHERE c.boardId = :boardId ORDER BY c.position ASC")
  List<BoardColumn> findAllByBoardIdOrderByPositionAscPessLock(@Param("boardId") UUID boardId);
}
