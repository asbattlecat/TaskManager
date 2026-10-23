package org.example.taskmanager.taskmanager.domain.aggregate;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.BoardType;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Getter
@Entity
@Validated
@NoArgsConstructor
@Table(name = "board")
public class Board extends AggregateRoot {
  @Id
  private UUID id;

  @Column(nullable = false)
  private ArchiveState archiveState;

  @Column(nullable = false, updatable = false)
  private UUID projectId;

  @Column(nullable = false)
  private String name;

  @Column
  private String description;

  @Column(nullable = false, updatable = false)
  private BoardType type;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt = null;

  @Version
  private Long version;

  public Board(@NotNull UUID projectId, @NotBlank String name, String description, @NotNull BoardType type) {
    id = UUID.randomUUID();

    archiveState = ArchiveState.ACTIVE;

    this.projectId = projectId;
    this.name = name;
    this.description = description;
    this.type = type;

    createdAt = Instant.now();
    log.debug("board created, id={}", id);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (!(object instanceof Board board)) return false;

    return id != null && Objects.equals(id, board.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  // ========= работа с архивацией/разархивацией =========

  public void archive(ArchiveReason reason) {
    requireActive("archive");

    if (reason == ArchiveReason.USER_ACTION) {
      archiveState = ArchiveState.USER_ARCHIVED;
    } else {
      archiveState = ArchiveState.PARENT_ARCHIVED;
    }

    log.debug("board archived, id={}", id);
//    registerEvent(new BoardArchivedEvent(id));

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
//    registerEvent(new BoardUnarchivedEvent(id));

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

  public void changeName(@NotBlank String newName) {
    if (Objects.equals(name, newName)) return;
    requireActive("changeName");

    String old = name;
    this.name = newName;
    touch();
    log.debug("board changeName completed, id={}, old={}, new={}", id, old, newName);
  }

  public void changeDescription(String newDescription) {
    if (Objects.equals(description, newDescription)) return;
    requireActive("changeDescription");

    String old = description;
    this.description = newDescription;
    touch();
    log.debug("board changeDescription completed, if={}, old={}, new={}", id, old, newDescription);
  }

  // ====== helpers =========

  private void touch() {
    updatedAt = Instant.now();
  }

  private void requireActive(String operationName) {
    if (archiveState != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Cannot do " + operationName
              + ", board is archived, id=" + id);
    }
  }
}
