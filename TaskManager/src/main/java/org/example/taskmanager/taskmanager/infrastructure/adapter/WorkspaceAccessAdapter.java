package org.example.taskmanager.taskmanager.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.aggregate.Workspace;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.port.WorkspaceAccessPort;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class WorkspaceAccessAdapter implements WorkspaceAccessPort {
  private final WorkspaceRepository workspaceRepository;

  public WorkspaceAccessAdapter(WorkspaceRepository workspaceRepository) {
    this.workspaceRepository = workspaceRepository;
  }

  @Override
  public boolean exists(UUID workspaceId) {
    return workspaceRepository.existsById(workspaceId);
  }

  @Override
  public boolean isActive(UUID workspaceId) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found, id=" + workspaceId));
    return workspace.getArchiveState() == ArchiveState.ACTIVE;
  }

  @Override
  public void requireActive(UUID workspaceId, String operationName) {
    if (!isActive(workspaceId)) {
      throw new IllegalStateException("Cannot do " + operationName
              + " because workspace with id=" + workspaceId + " is archived");
    }
  }
}
