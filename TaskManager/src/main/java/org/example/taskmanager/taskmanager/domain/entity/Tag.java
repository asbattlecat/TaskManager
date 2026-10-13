package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.taskmanager.taskmanager.domain.enums.TagColor;

import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
public class Tag {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id", nullable = false, updatable = false)
  private Task task;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private TagColor color;

  public Tag(@NotNull Task task, @NotNull String name, @NotNull TagColor color) {
    id = UUID.randomUUID();

    this.task = task;
    this.name = name;
    this.color = color;
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) return true;
    if (!(object instanceof Tag tag)) return false;

    return id != null && id.equals(tag.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
