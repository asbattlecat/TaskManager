package org.example.taskmanager.taskmanager.repository;

import jakarta.persistence.LockModeType;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
  List<Project> findAllByWorkspaceId(UUID workspaceId);

  // ================== archive =====================

  @Modifying(clearAutomatically = true)
  @Query(value = """ 
      UPDATE project
      SET archive_state = 'PARENT_ARCHIVED',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      WHERE workspace_id = :workspaceId
          AND archive_state = 'ACTIVE'
      RETURNING id
  """, nativeQuery = true)
  List<UUID> archiveByWorkspace(@Param("workspaceId") UUID workspaceId);

  // ================ unarchive ======================

  @Modifying(clearAutomatically = true)
  @Query(value = """
      UPDATE project
      SET archive_state = 'ACTIVE',
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      WHERE workspace_id = :workspaceId
          AND archive_state = 'PARENT_ARCHIVED'
      RETURNING id
  """, nativeQuery = true)
  List<UUID> unarchiveByWorkspace(@Param("workspaceId") UUID workspaceId);
}
