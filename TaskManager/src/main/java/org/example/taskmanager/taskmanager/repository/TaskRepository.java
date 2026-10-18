package org.example.taskmanager.taskmanager.repository;

import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
  List<Task> findAllByBoardId(UUID boardId);

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE task
      SET archive_state = 'PARENT_ARCHIVED',
          updated_at = CURRENT_TIMESTAMP
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
          updated_at = CURRENT_TIMESTAMP
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
          updated_at = CURRENT_TIMESTAMP
      WHERE board_id = :boardId
          AND archive_state = 'ACTIVE'
  """, nativeQuery = true)
  List<UUID> archiveByBoard(@Param("boardId") UUID boardId);
}
