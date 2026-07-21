package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.taskmanager.taskmanager.domain.enums.BoardType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Board {
  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID projectId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private BoardType type;

  @Column(nullable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt;

  public Board(@NotNull UUID projectId, @NotNull String name, @NotNull BoardType type) {
    id = UUID.randomUUID();

    this.projectId = projectId;
    this.name = name;
    this.type = type;

    createdAt = Instant.now();
  }
}
