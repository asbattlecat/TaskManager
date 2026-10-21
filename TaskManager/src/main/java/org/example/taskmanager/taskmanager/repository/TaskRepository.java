package org.example.taskmanager.taskmanager.repository;

import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
  List<Task> findAllByBoardId(UUID boardId);

  // ================= archive =================

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE task
      SET archive_state = 'PARENT_ARCHIVED',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      WHERE board_id IN (
          SELECT b.id
          FROM board AS b
          JOIN project AS p ON p.id = b.project_id
          WHERE p.workspace_id = :workspaceId
      )
          AND archive_state = 'ACTIVE'
      RETURNING id
  """, nativeQuery = true)
  List<UUID> archiveByWorkspace(@Param("workspaceId") UUID workspaceId);

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE task
      SET archive_state = 'PARENT_ARCHIVED',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      WHERE board_id IN (
          SELECT id
          FROM board
          WHERE project_id = :projectId
      )
          AND archive_state = 'ACTIVE'
      RETURNING id
  """, nativeQuery = true)
  List<UUID> archiveByProject(@Param("projectId") UUID projectId);

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE task
      SET archive_state = 'PARENT_ARCHIVED',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      WHERE board_id = :boardId
          AND archive_state = 'ACTIVE'
      RETURNING id
  """, nativeQuery = true)
  List<UUID> archiveByBoard(@Param("boardId") UUID boardId);

  // ============ unarchive ===============

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE task AS t
      SET archive_state = 'ACTIVE',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      FROM board AS b
      JOIN project AS p ON p.id = b.project_id
      WHERE t.board_id = b.id
          AND p.workspace_id = :workspaceId
          AND b.archive_state = 'ACTIVE'
          AND t.archive_state = 'PARENT_ARCHIVED'
      RETURNING t.id
  """, nativeQuery = true)
  List<UUID> unarchiveByWorkspace(@Param("workspaceId") UUID workspaceId);

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE task AS t
      SET archive_state = 'ACTIVE',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      FROM board AS b
      WHERE t.board_id = b.id
          AND b.project_id = :projectId
          AND b.archive_state = 'ACTIVE'
          AND t.archive_state = 'PARENT_ARCHIVED'
      RETURNING t.id
  """, nativeQuery = true)
  List<UUID> unarchiveByProject(@Param("projectId") UUID projectId);

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE task
      SET archive_state = 'ACTIVE',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      WHERE board_id = :boardId
          AND archive_state = 'PARENT_ARCHIVED'
      RETURNING id
  """, nativeQuery = true)
  List<UUID> unarchiveByBoard(@Param("boardId") UUID boardId);
}
