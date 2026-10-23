package org.example.taskmanager.taskmanager.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Entity
@Getter
@Validated
@NoArgsConstructor
public class Workspace extends AggregateRoot {
  @Id
  private UUID id;

  @Column(nullable = false)
  private ArchiveState archiveState;

  @Column(nullable = false)
  private String name;

  @Column
  private String description;

  @Column(nullable = false)
  private UUID ownerId;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt;

  @Version
  private Long version;


  public Workspace(@NotBlank String name, String description, @NotNull UUID ownerId) {
    id =  UUID.randomUUID();

    archiveState = ArchiveState.ACTIVE;

    this.name = name;
    this.description = description;
    this.ownerId = ownerId;

    this.createdAt = Instant.now();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (!(object instanceof Workspace workspace)) return false;

    return id != null && Objects.equals(id, workspace.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  // ============== архивация / разархивация ================

  public void archive(@NotNull ArchiveReason reason) {
    requireActive("archive");

    if (reason == ArchiveReason.USER_ACTION) {
      archiveState = ArchiveState.USER_ARCHIVED;
    } else {
      archiveState = ArchiveState.PARENT_ARCHIVED;
    }

//    registerEvent(new WorkspaceArchivedEvent(id));
    log.debug("workspace archived, id={}", id);
  }


  public void unarchive(@NotNull UnarchiveReason reason) {
    if (archiveState == ArchiveState.ACTIVE) {
      throw new IllegalStateException("Workspace is already active, cannot unarchive");
    }

    if (!canBeUnarchived(reason)) throw new IllegalStateException("Project cannot be unarchived");

    archiveState = ArchiveState.ACTIVE;

//    registerEvent(new WorkspaceUnarchivedEvent(id));
    log.debug("workspace unarchived, id={}", id);
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

  // ============ изменение полей ==============

  public void changeName(@NotBlank String newName) {
    if (Objects.equals(this.name, newName)) return;
    requireActive("changeName");

    String old = this.name;
    this.name = newName;

    touch();
    log.debug("workspace changeName completed, id={}, old={}, new={}", id, old, newName);
  }

  public void changeDescription(String newDescription) {
    if (Objects.equals(this.description, newDescription)) return;
    requireActive("changeDescription");

    String old = this.description;
    this.description = newDescription;

    touch();
    log.debug("workspace changeDescription completed, id={}, old={}, new={}",
            id, old, newDescription);
  }

  public void changeOwnerId(@NotNull UUID newOwnerId) {
    if (Objects.equals(this.ownerId, newOwnerId)) return;
    requireActive("changeOwnerId");

    UUID old = this.ownerId;
    this.ownerId = newOwnerId;

    touch();
    log.debug("workspace changeOwnerId completed, id={}, old={}, new={}", id, old, newOwnerId);
  }

  // =========== helpers =============

  private void requireActive(String operationName) {
    if (archiveState != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Cannot do " + operationName
              + ", workspace is archived, id=" + id);
    }
  }

  private void touch() {
    this.updatedAt = Instant.now();
  }
}
