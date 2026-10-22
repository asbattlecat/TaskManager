package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Getter
@NoArgsConstructor
@Entity
@Validated
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

  @Column(name = "board_id", nullable = false, updatable = false)
  private UUID boardId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Integer position;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt = null;

  @Version
  private Long version;

  public BoardColumn(@NotNull UUID boardId, @NotBlank String name, @NotNull Integer position) {
    id = UUID.randomUUID();

    this.boardId = boardId;
    this.name = name;
    this.position = position;
    this.createdAt = Instant.now();
  }

  public void changeName(@NotBlank String newName) {
    if (Objects.equals(name, newName)) return;

    String old = name;
    this.name = newName;

    touch();
    log.debug("BoardColumn changeName completed, id={}, old={}, new={}", id, old, newName);
  }

  public void changePosition(@NotNull Integer newPosition) {
    if (Objects.equals(position, newPosition)) return;

    Integer old = position;
    this.position = newPosition;

    touch();
    log.debug("BoardColumn changePosition completed, id={}, old={}, new={}", id, old, newPosition);
  }

  // ============ helpers ===============

  private void touch() {
    this.updatedAt = Instant.now();
  }
}
