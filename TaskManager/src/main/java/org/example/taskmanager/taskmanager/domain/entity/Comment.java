package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Getter
@NoArgsConstructor
@Entity
@Validated
public class Comment {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id", nullable = false, updatable = false)
  private Task task;

  @Column(nullable = false, updatable = false)
  private UUID authorId;

  @Column(nullable = false)
  private String content;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt = null;

  public Comment(@NotNull Task task, @NotNull UUID authorId, @NotBlank String content) {
    id = UUID.randomUUID();

    this.task = task;
    this.authorId = authorId;
    this.content = content;

    this.createdAt = Instant.now();
  }

  public void changeContent(@NotBlank String newContent) {
    if (Objects.equals(this.content, newContent)) return;

    String old = content;
    this.content = newContent;
    touch();

    log.debug("comment changeContent completed, id={}, old={}, new={}", id, old, newContent);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (!(object instanceof Comment comment)) return false;
    return id != null && id.equals(comment.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  // =================== helpers ===============

  private void touch() {
    updatedAt = Instant.now();
  }
}
