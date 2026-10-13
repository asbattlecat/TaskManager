package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Getter
@NoArgsConstructor
@Entity
public class Comment {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id", nullable = false, updatable = false)
  private Task task;

  @Column(nullable = false)
  private UUID authorId;

  @Column(nullable = false)
  private String content;

  @Column(nullable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt;

  public Comment(@NotNull Task task, @NotNull UUID authorId, @NotNull String content) {
    id = UUID.randomUUID();

    this.task = task;
    this.authorId = authorId;
    this.content = content;

    this.createdAt = Instant.now();
    updatedAt = Instant.now();
  }

  public void changeContent(String content) {
    if (Objects.equals(this.content, content)) return;

    this.content = content;
    updatedAt = Instant.now();
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
}
