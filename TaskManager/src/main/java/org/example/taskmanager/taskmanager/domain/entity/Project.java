package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.taskmanager.taskmanager.domain.event.ProjectArchivedEvent;
import org.example.taskmanager.taskmanager.domain.event.ProjectUnarchivedEvent;
import org.example.taskmanager.taskmanager.domain.aggregate.AggregateRoot;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Project extends AggregateRoot {
  @Id
  private UUID id;

  @Column(nullable = false)
  private ArchiveState archiveState;

  @Column(nullable = false)
  private UUID workspaceId;

  @Column(nullable = false)
  private String name;

  @Column
  private String description;

  @Column(nullable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt;

  @Version
  private Long version;

  public Project(@NotNull UUID workspaceId, @NotNull String name, String description) {
    id = UUID.randomUUID();

    archiveState = ArchiveState.ACTIVE;

    this.workspaceId = workspaceId;
    this.name = name;
    this.description = description;

    this.createdAt = Instant.now();
  }

  public void archive(@NotNull ArchiveReason reason) {
    if (archiveState != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Project is already archived");
    }

    if (reason == ArchiveReason.USER_ACTION) {
      archiveState = ArchiveState.USER_ARCHIVED;
    } else {
      archiveState = ArchiveState.PARENT_ARCHIVED;
    }

    registerEvent(new ProjectArchivedEvent(id));
  }

  public void unarchive(@NotNull UnarchiveReason reason) {
    if (archiveState == ArchiveState.ACTIVE) {
      throw new IllegalStateException("Project is already active, cannot unarchive");
    }

    if (!canBeUnarchived(reason)) throw new IllegalStateException("Project cannot be unarchived");

    archiveState = ArchiveState.ACTIVE;

    registerEvent(new ProjectUnarchivedEvent(id));
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