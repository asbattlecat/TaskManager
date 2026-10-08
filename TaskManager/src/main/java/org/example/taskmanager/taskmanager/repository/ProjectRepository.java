package org.example.taskmanager.taskmanager.repository;

import jakarta.persistence.LockModeType;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
  List<Project> findAllByWorkspaceId(UUID workspaceId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
    SELECT p 
    FROM project AS p 
    WHERE p.workspaceId = :workspaceId
  """)
  List<Project> findProjectsByWorkspaceIdWithPessimisticWriteLock(@Param("workspaceId")UUID workspaceId);
}
