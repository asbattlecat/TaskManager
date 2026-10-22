package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.event.TaskArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.TaskUnarchivedEvent;
import org.example.taskmanager.taskmanager.domain.aggregate.AggregateRoot;
import org.example.taskmanager.taskmanager.domain.enums.*;
import org.example.taskmanager.taskmanager.infrastructure.exception.AlreadyExistsException;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.*;

@Getter
@Entity
@NoArgsConstructor
@Slf4j
@Validated
public class Task extends AggregateRoot {
  @Id
  private UUID id;

  @Column(nullable = false)
  private ArchiveState archiveState;

  @Column(nullable = false)
  private UUID boardId;

  // ========== для kanban ==========
  @Column
  private UUID columnId;
  // ========== для kanban ==========

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

  @Column(nullable = false, updatable = false)
  private UUID creatorId;

  @Column
  private Instant deadline;

  @Column(nullable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt = null;

  @Version
  private Long version;

  @OneToMany(
          mappedBy = "task",
          cascade = CascadeType.ALL,
          orphanRemoval = true,
          fetch = FetchType.LAZY
  )
  private List<Comment> comments = new ArrayList<>();

  @OneToMany(
          mappedBy = "task",
          cascade = CascadeType.ALL,
          orphanRemoval = true,
          fetch = FetchType.LAZY
  )
  private Set<Tag> tags = new HashSet<>();

  public Task(@NotNull UUID boardId, UUID columnId, @NotBlank String name, String description,
              @NotNull TaskStatus status, @NotNull TaskPriority priority, UUID assigneeId,
              @NotNull UUID creatorId, Instant deadline) {
    if (status != TaskStatus.TODO) {
      throw new IllegalArgumentException("Cannot create task with status " + status + ", must be TO_DO");
    }
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

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (!(object instanceof Task task)) return false;
    return id != null && id.equals(task.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  // =========== архивация =============

  public void archive(ArchiveReason reason) {
    log.info("task archive operation starts, id={}, reason={}", id, reason);
    if (archiveState != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Task is already archived");
    }

    if (reason == ArchiveReason.USER_ACTION) {
      archiveState = ArchiveState.USER_ARCHIVED;
    } else {
      archiveState = ArchiveState.PARENT_ARCHIVED;
    }

    log.info("task archived, id={}, reason={}", id, reason);
    registerEvent(new TaskArchivedEvent(id));
    touch();
  }

  public void unarchive(UnarchiveReason reason) {
    log.info("task unarchive operation starts, id={}, reason={}", id, reason);
    if (archiveState == ArchiveState.ACTIVE) {
      throw new IllegalStateException("Task is already active, cannot unarchive");
    }

    if (!canBeUnarchived(reason)) throw new IllegalStateException("Task cannot be unarchived");

    archiveState = ArchiveState.ACTIVE;

    log.info("task unarchived, id={}, reason={}", id, reason);
    registerEvent(new TaskUnarchivedEvent(id));
    touch();
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

  // ============= изменение полей =========================

  public void changeColumnId(UUID newColumnId) {
    if (Objects.equals(columnId, newColumnId)) return;
    requireActive("changeColumnId");

    UUID old = columnId;
    this.columnId = newColumnId;
    touch();
    log.debug("task changeColumnId completed, id={}, old={}, new={}", id, old, newColumnId);
  }

  public void changeName(@NotBlank String newName) {
    if (Objects.equals(name, newName)) return;
    requireActive("changeName");

    String old = name;
    this.name = newName;
    touch();
    log.debug("task changeName completed, id={}, old={}, new={}", id, old, newName);
  }

  public void changeDescription(String newDescription) {
    if (Objects.equals(description, newDescription)) return;
    requireActive("changeDescription");

    String old = description;
    this.description = newDescription;
    touch();
    log.debug("task changeDescription completed, id={}, old={}, new={}", id, old, newDescription);
  }

  public void changeStatus(TaskStatus newStatus) {
    log.info("task changeStatus operation starts, id={}, newStatus={}", id, newStatus);
    if (status == newStatus) return;
    requireActive("changeStatus");

    TaskStatus old = status;
    this.status = newStatus;
    touch();
    log.debug("task changeStatus completed, id={}, old={}, new={}", id, old, newStatus);
  }

  public void changePriority(TaskPriority newPriority) {
    if (priority == newPriority) return;
    requireActive("changePriority");

    TaskPriority old = priority;
    this.priority = newPriority;
    touch();
    log.debug("task changePriority completed, id={}, old={}, new={}", id, old, newPriority);
  }

  public void changeAssigneeId(UUID newAssigneeId) {
    log.info("task changeAssigneeId operation starts, id={}, newAssigneeId={}", id, newAssigneeId);
    if(Objects.equals(assigneeId, newAssigneeId)) return;
    requireActive("changeAssigneeId");

    UUID old = assigneeId;
    this.assigneeId = newAssigneeId;
    touch();
    log.debug("task changeAssignee completed, id={}, old={}, new={}", id, old, newAssigneeId);
  }

  public void changeDeadline(Instant newDeadline) {
    if (Objects.equals(deadline, newDeadline)) return;
    requireActive("changeDeadline");

    Instant old = deadline;
    this.deadline = newDeadline;
    touch();
    log.debug("task changeDeadline completed, id={}, old={}, new={}", id, old, newDeadline);
  }

  public Comment addComment(UUID authorId, String content) {
    requireActive("addComment");

    if (authorId == null || content == null)
      throw new IllegalArgumentException("Task addComment operation: authorId or content is null");

    Comment comment = new Comment(this, authorId, content);
    comments.add(comment);
    touch();
    log.debug("task addComment completed, id={}, commentId={}", id, comment.getId());
    return comment;
  }

  public Tag addTag(String name, TagColor color) {
    requireActive("addTag");

    if (name == null || color == null)
      throw new IllegalArgumentException("Task addTag operation: name or color is null");

    boolean exists = tags.stream()
            .anyMatch(t -> t.getName().equalsIgnoreCase(name));
    if (exists)
      throw new AlreadyExistsException("Tag already exists, tag name:" + name);

    Tag tag = new Tag(this, name, color);
    tags.add(tag);
    touch();

    log.debug("task addTag completed, id={}, tagId={}", id, tag.getId());
    return tag;
  }

  // ===================== вспомогательные методы =====================

  private void requireActive(String operationName) {
    if (archiveState != ArchiveState.ACTIVE)
      throw new IllegalStateException("Cannot do " + operationName + ", task is archived, id=" + id);
  }

  private void touch() {
    updatedAt = Instant.now();
  }
}
