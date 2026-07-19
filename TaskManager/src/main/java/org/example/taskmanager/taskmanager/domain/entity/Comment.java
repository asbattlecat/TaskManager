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
public class Comment {
  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID taskId;

  @Column(nullable = false)
  private UUID authorId;

  @Column(nullable = false)
  private String content;

  @Column(nullable = false)
  private Instant cratedAt;

  @Column
  private Instant updatedAt;

  public Comment(UUID taskId, UUID authorId, String content, Instant cratedAt) {
    id = UUID.randomUUID();
    this.taskId = taskId;
    this.authorId = authorId;
    this.content = content;
    this.cratedAt = cratedAt;
    updatedAt = null;
  }
}
