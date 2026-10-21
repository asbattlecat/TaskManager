package org.example.taskmanager.taskmanager.repository;

import jakarta.persistence.LockModeType;
import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {
  List<Board> findAllByProjectId(UUID projectId);

  // =============== archive =======================

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE board
      SET archive_state = 'PARENT_ARCHIVED',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      WHERE project_id IN (
          SELECT id
          FROM project
          WHERE workspace_id = :workspaceId
      )
          AND archive_state = 'ACTIVE'
      RETURNING id
  """, nativeQuery = true)
  List<UUID> archiveByWorkspace(@Param("workspaceId") UUID workspaceId);

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE board
      SET archive_state = 'PARENT_ARCHIVED',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      WHERE project_id = :projectId
          AND archive_state = 'ACTIVE'
      RETURNING id
  """, nativeQuery = true)
  List<UUID> archiveByProject(@Param("projectId") UUID projectId);

  // ================= unarchive ====================

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE board AS b
      SET archive_state = 'ACTIVE',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      FROM project AS p
      WHERE b.project_id = p.id
          AND p.workspace_id = :workspaceId
          AND p.archive_state = 'ACTIVE'
          AND b.archive_state = 'PARENT_ARCHIVED'
      RETURNING b.id
  """, nativeQuery = true)
  List<UUID> unarchiveByWorkspace(@Param("workspaceId") UUID workspaceId);

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE board
      SET archive_state = 'ACTIVE',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      WHERE project_id = :projectId
          AND archive_state = 'PARENT_ARCHIVED'
      RETURNING id
  """, nativeQuery = true)
  List<UUID> unarchiveByProject(@Param("projectId") UUID projectId);
}
