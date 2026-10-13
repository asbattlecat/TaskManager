package org.example.taskmanager.taskmanager.repository;

import org.example.taskmanager.taskmanager.domain.entity.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEntityRepository extends JpaRepository<AuditEntry, UUID> {
  List<AuditEntry> findAllByTaskIdOrderByTimestampAsc(UUID taskId);
}
