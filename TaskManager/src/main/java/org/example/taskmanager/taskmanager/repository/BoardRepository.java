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

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE board
      SET archive_state = 'PARENT_ARCHIVED',
          updated_at = CURRENT_TIMESTAMP
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
          updated_at = CURRENT_TIMESTAMP
      WHERE project_id = :projectId
          AND archive_state = 'ACTIVE'
      RETURNING id
  """, nativeQuery = true)
  List<UUID> archiveByProject(@Param("projectId") UUID projectId);

  List<Board> findAllByProjectId(UUID projectId);
}
