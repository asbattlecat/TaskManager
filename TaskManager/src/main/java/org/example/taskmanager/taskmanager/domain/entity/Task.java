package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

  // для kanban
  @Column
  private UUID columnId;

  @Column(nullable = false)
  private String name;

  @Column
  private String description;

  @Column(nullable = false)
  private TaskStatus status;

  @Column(nullable = false)
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

  private List<Task> subtasks;

  @Column
  private List<Tag> tags;

  @Column
  private List<Comment> comments;

  @Column
  private List<AuditEntity> auditEntities;

  public Task(@NotNull UUID boardId, UUID columnId, @NotNull String name, String description, TaskStatus status,
              TaskPriority priority, UUID assigneeId, @NotNull  UUID creatorId, Instant deadline) {
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
    updatedAt = null;
    subtasks = new ArrayList<>();
    tags = new ArrayList<>();
    comments = new ArrayList<>();
    auditEntities = new ArrayList<>();
  }

  public void addSubtask(Task subtask) {
    subtasks.add(subtask);
  }

  public void addTag(Tag tag) {
    tags.add(tag);
  }

  public void addComment(Comment comment) {
    comments.add(comment);
  }

  public void addAuditEntity(AuditEntity auditEntity) {
    auditEntities.add(auditEntity);
  }

  public Task removeSubtask(int index) {
    return subtasks.remove(index);
  }

  public Tag removeTag(int index) {
    return tags.remove(index);
  }

  public Comment removeComment(int index) {
    return comments.remove(index);
  }

  public AuditEntity removeAuditEntity(int index) {
    return auditEntities.remove(index);
  }
}
