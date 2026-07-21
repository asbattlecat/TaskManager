package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.taskmanager.taskmanager.domain.enums.TagColor;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Tag {
  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID taskId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private TagColor color;

  public Tag(UUID taskId, String name, TagColor color) {
    id = UUID.randomUUID();

    this.taskId = taskId;
    this.name = name;
    this.color = color;
  }
}
