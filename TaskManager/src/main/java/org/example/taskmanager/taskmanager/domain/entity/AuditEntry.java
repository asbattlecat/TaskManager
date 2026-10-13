package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "audit_entity")
@NoArgsConstructor
@Immutable
public class AuditEntry {
  @Id
  private UUID id;

  @Column(nullable = false, updatable = false)
  private UUID taskId;

  @Column(nullable = false, updatable = false)
  private UUID userId;

  @Column(nullable = false, updatable = false)
  private String fieldName;

  @Column(nullable = false, updatable = false)
  private String oldValue;

  @Column(nullable = false, updatable = false)
  private String newValue;

  @Column(nullable = false, updatable = false)
  private Instant timestamp;

  public AuditEntry(@NotNull UUID taskId, @NotNull UUID userId, @NotNull String fieldName, @NotNull String oldValue,
                    @NotNull String newValue) {
    id = UUID.randomUUID();

    this.taskId = taskId;
    this.userId = userId;
    this.fieldName = fieldName;
    this.oldValue = oldValue;
    this.newValue = newValue;

    this.timestamp = Instant.now();
  }
}
