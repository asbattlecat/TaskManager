package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.event.ProjectArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.ProjectUnarchivedEvent;
import org.example.taskmanager.taskmanager.domain.aggregate.AggregateRoot;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "project")
@Slf4j
@Validated
public class Project extends AggregateRoot {
  @Id
  private UUID id;

  @Column(nullable = false)
  private ArchiveState archiveState;

  @Column(nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(nullable = false)
  private String name;

  @Column
  private String description;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt = null;

  @Version
  private Long version;

  public Project(@NotNull UUID workspaceId, @NotBlank String name, String description) {
    id = UUID.randomUUID();

    archiveState = ArchiveState.ACTIVE;

    this.workspaceId = workspaceId;
    this.name = name;
    this.description = description;

    this.createdAt = Instant.now();
  }

  // =============== архивация/разархивация ==============

  public void archive(@NotNull ArchiveReason reason) {
    log.info("task archive starts, id={}, reason={}", id, reason);
    requireActive("archive");

    if (reason == ArchiveReason.USER_ACTION) {
      archiveState = ArchiveState.USER_ARCHIVED;
    } else {
      archiveState = ArchiveState.PARENT_ARCHIVED;
    }

//    registerEvent(new ProjectArchivedEvent(id));
    touch();
    log.debug("project archived, id={}", id);
  }

  public void unarchive(@NotNull UnarchiveReason reason) {
    log.info("task unarchive starts, id={}, reason={}", id, reason);
    if (archiveState == ArchiveState.ACTIVE) {
      throw new IllegalStateException("Project is already active, cannot unarchive");
    }

    if (!canBeUnarchived(reason)) throw new IllegalStateException("Project cannot be unarchived");

    archiveState = ArchiveState.ACTIVE;

//    registerEvent(new ProjectUnarchivedEvent(id));
    touch();
    log.debug("project unarchived, id={}", id);
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

  // =============== изменение полей ==============

  public void changeName(@NotBlank String newName) {
    if (Objects.equals(name, newName)) return;

    requireActive("changeName");
    String old = name;
    this.name = newName;

    touch();
    log.debug("project changeName completed, id={}, old={}, new={}", id, old, newName);
  }

  public void changeDescription(String newDescription) {
    if (Objects.equals(description, newDescription)) return;

    requireActive("changeDescription");
    String old = this.description;
    this.description = newDescription;

    touch();
    log.debug("project changeDescription completed, id={}, old={}, new={}", id, old, newDescription);
  }

  // ============= helpers =================

  private void requireActive(String operationName) {
    if (archiveState != ArchiveState.ACTIVE)
      throw new IllegalStateException("Cannot do " + operationName
              + ", project is archived, id=" + id);
  }

  private void touch() {
    this.updatedAt = Instant.now();
  }
}