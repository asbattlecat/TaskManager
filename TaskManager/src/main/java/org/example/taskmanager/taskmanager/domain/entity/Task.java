package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.taskmanager.taskmanager.domain.enums.TaskPriority;
import org.example.taskmanager.taskmanager.domain.enums.TaskStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Task {
  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID boardId;

  // --------- для kanban ----------
  @Column
  private UUID columnId;
  // --------- для kanban ----------

  @Column(nullable = false)
  private String name;

  @Column
  private String description;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private TaskStatus status;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private TaskPriority priority;

  @Column
  private UUID assigneeId;

  @Column(nullable = false)
  private UUID creatorId;

  @Column
  private Instant deadline;

  @Column(nullable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt;

  public Task(@NotNull UUID boardId, UUID columnId, @NotNull String name, String description,
              @NotNull TaskStatus status, @NotNull TaskPriority priority, UUID assigneeId,
              @NotNull UUID creatorId, Instant deadline) {
    id = UUID.randomUUID();

    this.boardId = boardId;
    this.columnId = columnId;
    this.name = name;
    this.description = description;
    this.status = status;
    this.priority = priority;
    this.assigneeId = assigneeId;
    this.creatorId = creatorId;
    this.deadline = deadline;

    createdAt = Instant.now();
  }
}
