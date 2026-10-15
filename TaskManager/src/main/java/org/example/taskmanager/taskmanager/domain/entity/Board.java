package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.event.BoardArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.BoardUnarchivedEvent;
import org.example.taskmanager.taskmanager.domain.aggregate.AggregateRoot;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.BoardType;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Slf4j
public class Board extends AggregateRoot {
  @Id
  private UUID id;

  @Column(nullable = false)
  private ArchiveState archiveState;

  @Column(nullable = false)
  private UUID projectId;

  @Column(nullable = false)
  private String name;

  @Column
  private String description;

  @Column(nullable = false, updatable = false)
  private BoardType type;

  @Column(nullable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt;

  @Version
  private Long version;

  public Board(@NotNull UUID projectId, @NotNull String name, String description, @NotNull BoardType type) {
    id = UUID.randomUUID();

    archiveState = ArchiveState.ACTIVE;

    this.projectId = projectId;
    this.name = name;
    this.description = description;
    this.type = type;

    createdAt = Instant.now();
  }

  // ========= работа с архивацией/разархивацией =========

  public void archive(ArchiveReason reason) {
    if (archiveState != ArchiveState.ACTIVE) {
      log.error("cannot archive board, it is already archived, boardId={}", id);
      throw new IllegalStateException("Board is already archived");
    }

    if (reason == ArchiveReason.USER_ACTION) {
      archiveState = ArchiveState.USER_ARCHIVED;
    } else {
      archiveState = ArchiveState.PARENT_ARCHIVED;
    }

    log.debug("board archived, id={}", id);
    registerEvent(new BoardArchivedEvent(id));

    touch();
  }

  public void unarchive(UnarchiveReason reason) {
    if (archiveState == ArchiveState.ACTIVE) {
      log.error("cannot unarchive board, it is already active, boardId={}", id);
      throw new IllegalStateException("Board is already active, cannot unarchive");
    }
    if (!canBeUnarchived(reason))
      throw new IllegalStateException("Board cannot be unarchived");

    archiveState = ArchiveState.ACTIVE;

    log.debug("board unarchived, id={}", id);
    registerEvent(new BoardUnarchivedEvent(id));

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

  // =========== изменение полей =========

  public void changeName(String newName) {
    if (Objects.equals(name, newName)) return;
    checkIsActive("changeName");

    String old = name;
    this.name = newName;
    touch();
    log.debug("board changeName completed, id={}, old={}, new={}", id, old, newName);
  }

  public void changeDescription(String newDescription) {
    if (Objects.equals(description, newDescription)) return;
    checkIsActive("changeDescription");

    String old = description;
    this.description = newDescription;
    touch();
    log.debug("board changeDescription completed, if={}, old={}, new={}", id, old, newDescription);
  }

  // ====== helpers =========

  private void touch() {
    updatedAt = Instant.now();
  }

  private void checkIsActive(String operationName) {
    if (archiveState != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Cannot do " + operationName
              + ", board is archived, id=" + id);
    }
  }
}
