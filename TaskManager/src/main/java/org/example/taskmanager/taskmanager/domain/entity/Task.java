package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.taskmanager.taskmanager.domain.event.TaskArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.TaskUnarchivedEvent;
import org.example.taskmanager.taskmanager.domain.aggregate.AggregateRoot;
import org.example.taskmanager.taskmanager.domain.enums.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Task extends AggregateRoot {
  @Id
  private UUID id;

  @Column(nullable = false)
  private ArchiveState archiveState;

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

    archiveState = ArchiveState.ACTIVE;

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

  public void archive(ArchiveReason reason) {
    if (archiveState != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Task is already archived");
    }

    if (reason == ArchiveReason.USER_ACTION) {
      archiveState = ArchiveState.USER_ARCHIVED;
    } else {
      archiveState = ArchiveState.PARENT_ARCHIVED;
    }

    registerEvent(new TaskArchivedEvent(id));
  }

  public void unarchive(UnarchiveReason reason) {
    if (archiveState == ArchiveState.ACTIVE) {
      throw new IllegalStateException("Task is already active");
    }

    if (!canBeUnarchived(reason)) throw new IllegalStateException("Task cannot be unarchived");

    archiveState = ArchiveState.ACTIVE;

    registerEvent(new TaskUnarchivedEvent(id));
  }

  public boolean canBeUnarchived(UnarchiveReason reason) {
    return switch (archiveState) {
      case USER_ARCHIVED ->
              reason == UnarchiveReason.USER_ACTION;
      case PARENT_ARCHIVED ->
              true;
      case ACTIVE ->
              false;
    };
  }
}
