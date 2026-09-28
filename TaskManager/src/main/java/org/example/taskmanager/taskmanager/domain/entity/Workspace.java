package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.taskmanager.taskmanager.controller.dto.event.WorkspaceArchived;
import org.example.taskmanager.taskmanager.controller.dto.event.WorkspaceUnarchived;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Workspace {
  @Id
  private UUID id;

  @Column
  private boolean archived;

  @Column(nullable = false)
  private ArchiveReason archiveReason;

  @Column(nullable = false)
  private String name;

  @Column
  private String description;

  @Column(nullable = false)
  private UUID ownerId;

  @Column(nullable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt;


  public Workspace(@NotNull String name, String description, @NotNull UUID ownerId) {
    id =  UUID.randomUUID();

    archived = false;
    archiveReason = ArchiveReason.NONE;

    this.name = name;
    this.description = description;
    this.ownerId = ownerId;

    this.createdAt = Instant.now();
  }

  public WorkspaceArchived archive(@NotNull ArchiveReason reason) {
    if (archived) {
      throw new IllegalStateException("Cannot archive twice");
    }

    archived = true;
    archiveReason = reason;

    return new WorkspaceArchived(id, reason);
  }

  public WorkspaceUnarchived unarchive() {
    if (!archived) {
      throw new IllegalStateException("Cannot unarchive twice");
    }

    archived = false;
    archiveReason = ArchiveReason.NONE;

    return new WorkspaceUnarchived(id);
  }
}
