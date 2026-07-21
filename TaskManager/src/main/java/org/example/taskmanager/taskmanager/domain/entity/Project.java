package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Project {
  @Id
  private UUID id;

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

  @Column(nullable = false)
  private boolean archived;

  public Project(@NotNull UUID workspaceId, @NotNull String name, String description) {
    id = UUID.randomUUID();

    this.workspaceId = workspaceId;
    this.name = name;
    this.description = description;

    this.createdAt = Instant.now();
    archived = false;
  }
}
