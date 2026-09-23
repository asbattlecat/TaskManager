package org.example.taskmanager.taskmanager.repository;

import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
  List<Project> findAllByWorkspaceId(UUID workspaceId);
}
