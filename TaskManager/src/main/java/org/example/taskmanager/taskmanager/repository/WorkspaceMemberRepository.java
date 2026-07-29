package org.example.taskmanager.taskmanager.repository;

import org.example.taskmanager.taskmanager.domain.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
  boolean existsByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);
  List<WorkspaceMember> findAllByWorkspaceId(UUID workspaceId);
}
