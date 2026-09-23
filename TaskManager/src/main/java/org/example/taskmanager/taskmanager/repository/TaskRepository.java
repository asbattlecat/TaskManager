package org.example.taskmanager.taskmanager.repository;

import org.example.taskmanager.taskmanager.domain.entity.AuditEntity;
import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
  List<Task> findAllByBoardId(UUID boardId);
}
