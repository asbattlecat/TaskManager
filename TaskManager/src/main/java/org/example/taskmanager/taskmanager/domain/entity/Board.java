package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.taskmanager.taskmanager.controller.dto.event.BoardArchived;
import org.example.taskmanager.taskmanager.controller.dto.event.BoardUnarchived;
import org.example.taskmanager.taskmanager.domain.aggregate.AggregateRoot;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.BoardType;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
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

  @Column(nullable = false)
  private BoardType type;

  @Column(nullable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt;

  public Board(@NotNull UUID projectId, @NotNull String name, String description, @NotNull BoardType type) {
    id = UUID.randomUUID();

    archiveState = ArchiveState.ACTIVE;

    this.projectId = projectId;
    this.name = name;
    this.description = description;
    this.type = type;

    createdAt = Instant.now();
  }

  public void archive(ArchiveReason reason) {
    if (archiveState != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Board is already archived");
    }

    if (reason == ArchiveReason.USER_ACTION) {
      archiveState = ArchiveState.USER_ARCHIVED;
    } else {
      archiveState = ArchiveState.PARENT_ARCHIVED;
    }

    registerEvent(new BoardArchived(id, reason));
  }

  public void unarchive(UnarchiveReason reason) {
    if (archiveState == ArchiveState.ACTIVE) {
      throw new IllegalStateException("Board is already active");
    }

    archiveState = ArchiveState.ACTIVE;

    registerEvent(new BoardUnarchived(id, reason));
  }
}
