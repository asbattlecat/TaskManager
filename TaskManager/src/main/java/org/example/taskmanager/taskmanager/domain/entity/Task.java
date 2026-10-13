package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.event.TaskArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.TaskUnarchivedEvent;
import org.example.taskmanager.taskmanager.domain.aggregate.AggregateRoot;
import org.example.taskmanager.taskmanager.domain.enums.*;
import org.example.taskmanager.taskmanager.infrastructure.exception.AlreadyExistsException;

import java.time.Instant;
import java.util.*;

@Getter
@NoArgsConstructor
@Entity
@Slf4j
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

  @Column(nullable = false, updatable = false)
  private UUID creatorId;

  @Column
  private Instant deadline;

  @Column(nullable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt;

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
    updatedAt = Instant.now();
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
    checkIsActive("changeColumnId");

    UUID old = columnId;
    this.columnId = newColumnId;
    touch();
    log.debug("task changeColumnId operation completed, id={}, old={}, new={}", id, old, newColumnId);
  }

  public void changeName(String newName) {
    if (Objects.equals(name, newName)) return;
    checkIsActive("changeName");

    String old = name;
    this.name = newName;
    touch();
    log.debug("task changeName operation completed, id={}, old={}, new={}", id, old, newName);
  }

  public void changeDescription(String newDescription) {
    if (Objects.equals(description, newDescription)) return;
    checkIsActive("changeDescription");

    String old = description;
    this.description = newDescription;
    touch();
    log.debug("task changeDescription operation completed, id={}, old={}, new={}", id, old, newDescription);
  }

  public void changeStatus(TaskStatus newStatus) {
    log.info("task changeStatus operation starts, id={}, newStatus={}", id, newStatus);
    if (Objects.equals(status, newStatus)) return;
    checkIsActive("changeStatus");

    TaskStatus old = status;
    this.status = newStatus;
    touch();
    log.debug("task changeStatus operation completed, id={}, old={}, new={}", id, old, newStatus);
  }

  public void changePriority(TaskPriority newPriority) {
    if (Objects.equals(priority, newPriority)) return;
    checkIsActive("changePriority");

    TaskPriority old = priority;
    this.priority = newPriority;
    touch();
    log.debug("task changePriority operation completed, id={}, old={}, new={}", id, old, newPriority);
  }

  public void changeAssigneeId(UUID newAssigneeId) {
    log.info("task changeAssigneeId operation starts, id={}, newAssigneeId={}", id, newAssigneeId);
    if(Objects.equals(assigneeId, newAssigneeId)) return;
    checkIsActive("changeAssigneeId");

    UUID old = assigneeId;
    this.assigneeId = newAssigneeId;
    touch();
    log.debug("task changeAssignee operation completed, id={}, old={}, new={}", id, old, newAssigneeId);
  }

  public void changeDeadline(Instant newDeadline) {
    if (Objects.equals(deadline, newDeadline)) return;
    checkIsActive("changeDeadline");

    Instant old = deadline;
    this.deadline = newDeadline;
    touch();
    log.debug("task changeDeadline operation completed, id={}, old={}, new={}", id, old, newDeadline);
  }

  public Comment addComment(UUID authorId, String content) {
    checkIsActive("addComment");

    if (authorId == null || content == null) {
      throw new IllegalArgumentException("Task addComment operation: authorId or content is null");
    }

    Comment comment = new Comment(this, authorId, content);
    comments.add(comment);
    touch();
    log.debug("task addComment operation completed, id={}, commentId={}", id, comment.getId());
    return comment;
  }

  public Tag addTag(String name, TagColor color) {
    checkIsActive("addTag");

    if (name == null || color == null) {
      throw new IllegalArgumentException("Task addTag operation: name or color is null");
    }

    boolean exists = tags.stream()
            .anyMatch(t -> t.getName().equalsIgnoreCase(name));
    if (exists)
      throw new AlreadyExistsException("Tag already exists, tag name:" + name);

    Tag tag = new Tag(this, name, color);
    tags.add(tag);
    touch();
    log.debug("task addTag operation completed, id={}, tagId={}", id, tag.getId());

    return tag;
  }

  // ===================== вспомогательные методы =====================

  private void checkIsActive(String operationName) {
    if (archiveState != ArchiveState.ACTIVE)
      throw new IllegalStateException("Cannot do " + operationName + ", task is not active, id=" + id);
  }

  private void touch() {
    updatedAt = Instant.now();
  }
}
