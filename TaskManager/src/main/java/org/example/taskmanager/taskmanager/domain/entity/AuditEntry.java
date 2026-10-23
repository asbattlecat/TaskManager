package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Immutable;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// TODO чек 4 issue github
@Slf4j
@Getter
@Entity
@Immutable
@Validated
@NoArgsConstructor
@Table(name = "audit_entry")
public class AuditEntry {
  @Id
  private UUID id;

  @Column(nullable = false, updatable = false)
  private UUID taskId;

  @Column(updatable = false)
  private UUID memberId;

  @Column(nullable = false, updatable = false)
  private String fieldName;

  @Column(updatable = false)
  private String oldValue;

  @Column(updatable = false)
  private String newValue;

  @Column(nullable = false, updatable = false)
  private Instant timestamp;

  public AuditEntry(@NotNull UUID taskId, UUID memberId, @NotBlank String fieldName, String oldValue,
                    String newValue) {
    id = UUID.randomUUID();

    this.taskId = taskId;
    this.memberId = memberId;
    this.fieldName = fieldName;
    this.oldValue = oldValue;
    this.newValue = newValue;

    this.timestamp = Instant.now();

    log.debug("audit entry created, id={}", id);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (!(object instanceof AuditEntry auditEntry)) return false;

    return id != null && Objects.equals(id, auditEntry.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
