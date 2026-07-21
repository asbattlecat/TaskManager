package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
public class AuditEntity {
  @Id
  private final UUID id;

  @Column(nullable = false)
  private final UUID taskId;

  @Column(nullable = false)
  private final UUID userId;

  @Column(nullable = false)
  private final String fieldName;

  @Column(nullable = false)
  private final String oldValue;

  @Column(nullable = false)
  private final String newValue;

  @Column(nullable = false)
  private final Instant timestamp;

  public AuditEntity(@NotNull UUID taskId, @NotNull UUID userId, @NotNull String fieldName, @NotNull String oldValue,
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
