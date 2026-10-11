package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Immutable
@Table(name = "audit_entity")
public class AuditEntity {
  @Id
  private final UUID id;

  @Column(nullable = false, updatable = false)
  private final UUID taskId;

  @Column(updatable = false)
  private final UUID userId;

  @Column(nullable = false, updatable = false)
  private final String fieldName;

  @Column(nullable = false, updatable = false)
  private final String oldValue;

  @Column(nullable = false, updatable = false)
  private final String newValue;

  @Column(nullable = false, updatable = false)
  private final Instant timestamp;

  public AuditEntity(@NotNull UUID taskId, UUID userId, @NotNull String fieldName, @NotNull String oldValue,
                     @NotNull String newValue) {
    id = UUID.randomUUID();

    this.taskId = taskId;
    this.userId = userId;
    this.fieldName = fieldName;
    this.oldValue = oldValue;
    this.newValue = newValue;

    this.timestamp = Instant.now();
  }

  public AuditEntity() {
    id = null;
    taskId = null;
    userId = null;
    fieldName = null;
    oldValue = null;
    newValue = null;
    timestamp = null;
  }
}
