package org.example.taskmanager.taskmanager.repository;

import org.example.taskmanager.taskmanager.domain.entity.AuditEntity;
import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<AuditEntity, UUID> {
  List<Board> getBoardsByProjectId(UUID projectId);
}
