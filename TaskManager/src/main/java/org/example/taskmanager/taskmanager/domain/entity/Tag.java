package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.enums.TagColor;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Validated
@Slf4j
public class Tag {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id", nullable = false, updatable = false)
  private Task task;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private TagColor color;

  @Column
  private Instant updatedAt = null;

  @Version
  private Long version;

  public Tag(@NotNull Task task, @NotBlank String name, @NotNull TagColor color) {
    id = UUID.randomUUID();

    this.task = task;
    this.name = name;
    this.color = color;
  }

  public void changeName(@NotBlank String newName) {
    if (Objects.equals(name, newName)) return;

    String old = name;
    this.name = newName;

    touch();
    log.debug("Tag changeName completed, id={}, old={}, new={}", id, old, newName);
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) return true;
    if (!(object instanceof Tag tag)) return false;

    return id != null && id.equals(tag.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  // =========== helpers =================

  private void touch() {
    updatedAt = Instant.now();
  }
}
