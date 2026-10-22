package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "board_column",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_board_column_board_id_position",
                        columnNames = {
                                "board_id",
                                "position"
                        }
                )
        }
)
public class BoardColumn {
  @Id
  private UUID id;

  @Column(name = "board_id", nullable = false)
  private UUID boardId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Integer position;

  @Version
  private Long version;

  public BoardColumn(@NotNull UUID boardId, @NotNull String name, @NotNull Integer position) {
    id = UUID.randomUUID();

    this.boardId = boardId;
    this.name = name;
    this.position = position;
  }

  public void changeName(String newName) {
    if (Objects.equals(name, newName)) return;

    String old = name;
    this.name = newName;

    log.debug("BoardColumn changeName completed, id={}, old={}, new={}", id, old, newName);
  }

  public void changePosition(Integer newPosition) {
    if (Objects.equals(position, newPosition)) return;

    Integer old = position;
    this.position = newPosition;

    log.debug("BoardColumn changePosition completed, id={}, old={}, new={}", id, old, newPosition);
  }
}
