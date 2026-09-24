package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.domain.entity.Task;

import java.util.UUID;

public interface WorkspaceAccessChecker {
  void check(Task task, UUID workspaceMemberId);
}
