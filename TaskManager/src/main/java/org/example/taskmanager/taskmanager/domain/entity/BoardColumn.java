package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
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
}
