package org.example.taskmanager.taskmanager.domain;

import org.example.taskmanager.taskmanager.domain.enums.TaskPriority;
import org.example.taskmanager.taskmanager.domain.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class Task {
  private UUID id;
  private String header;
  private String description;
  private TaskStatus status;
  private TaskPriority priority;
  // TODO: исполнитель нужен сюда (User)
  private LocalDateTime deadline;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  // TODO: добавить тэги
  // TODO: добавить подзадачи Subtask
  // TODO: добавить историю изменений
}
