package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceMember {
  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID workspaceId;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private WorkspaceRole role;

  @Column(nullable = false)
  private Instant joinedAt;

  public WorkspaceMember(UUID workspaceId, UUID userId, WorkspaceRole role) {
    id = UUID.randomUUID();

    this.workspaceId = workspaceId;
    this.userId = userId;
    this.role = role;

    this.joinedAt = Instant.now();
  }
}
