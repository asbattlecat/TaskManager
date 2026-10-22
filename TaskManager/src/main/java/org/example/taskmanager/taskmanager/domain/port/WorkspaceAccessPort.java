package org.example.taskmanager.taskmanager.domain.port;

import java.util.UUID;

public interface WorkspaceAccessPort {
  boolean exists(UUID workspaceId);
  boolean isActive(UUID workspaceId);
  void requireActive(UUID workspaceId, String operationName);
}
