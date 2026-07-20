package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class AuditEntity {
  @Id
  private UUID id; //

  @Column
  private UUID taskId; //

  @Column(nullable = false)
  private UUID userId; //

  @Column(nullable = false)
  private Instant timestamp; //

  @Column(nullable = false)
  private String fieldName; //

  @Column(nullable = false)
  private String oldValue; //

  @Column(nullable = false)
  private String newValue; //

  @Column(nullable = false)
  private UUID workspaceId; //

  @Column
  private UUID projectId; //

  @Column
  private UUID boardId; //

  public AuditEntity(UUID taskId, UUID userId, String fieldName, String oldValue, String newValue,
                     UUID workspaceId, UUID projectId, UUID boardId) {
    this.taskId = taskId;
    this.userId = userId;
    this.timestamp = Instant.now();
    this.fieldName = fieldName;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.workspaceId = workspaceId;
    this.projectId = projectId;
    this.boardId = boardId;
  }
}
