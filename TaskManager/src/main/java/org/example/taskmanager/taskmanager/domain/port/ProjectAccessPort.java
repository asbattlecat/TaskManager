package org.example.taskmanager.taskmanager.domain.port;

import java.util.UUID;

public interface ProjectAccessPort {
  boolean exists(UUID projectId);
  boolean isActive(UUID projectId);
  void requireActive(UUID projectId, String operationName);
}
