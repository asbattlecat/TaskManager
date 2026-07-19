package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

  @Column
  private List<Board> boards;

  public Project(UUID workspaceId, String name, String description, Instant createdAt) {
    id = UUID.randomUUID();
    this.workspaceId = workspaceId;
    this.name = name;
    this.description = description;
    this.createdAt = createdAt;
    archived = false;
    boards = new ArrayList<>();
  }

  public void addBoard(Board board) {
    boards.add(board);
  }

  public void addBoardInPlace(Board board, int index) {
    // TODO: проверка на индекс
    boards.add(index, board);
  }
}
