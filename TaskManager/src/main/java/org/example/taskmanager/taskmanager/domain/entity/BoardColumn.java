package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class BoardColumn {
  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID boardId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Integer position;

  public BoardColumn(UUID boardId, String name, Integer position) {
    id = UUID.randomUUID();

    this.boardId = boardId;
    this.name = name;
    this.position = position;
  }
}
