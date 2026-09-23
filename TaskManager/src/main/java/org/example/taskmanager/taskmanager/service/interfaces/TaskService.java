package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.TaskDto;
import org.example.taskmanager.taskmanager.domain.entity.Tag;
import org.example.taskmanager.taskmanager.domain.enums.TagColor;
import org.example.taskmanager.taskmanager.domain.enums.TaskPriority;
import org.example.taskmanager.taskmanager.domain.enums.TaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaskService {
  TaskDto create(UUID boardId, UUID columnId, String name, String description, TaskStatus status,
                 TaskPriority priority, UUID assigneeId, UUID creatorId, Instant deadline);
  TaskDto changeName(UUID taskId, String newName);
  TaskDto changeDescription(UUID taskId, String newDescription);
  TaskDto changePriority(UUID taskId, TaskPriority newPriority);
  TaskDto changeColumn(UUID taskId, UUID newColumnId);
  TaskDto changeDeadline(UUID taskId, Instant newDeadline);
  TaskDto changeStatus(UUID taskId, TaskStatus newStatus);
  TaskDto setAssignee(UUID taskId, UUID assigneeId);
  void addComment(UUID taskId, UUID authorId, String content);
  void addTag(UUID taskId, String name, TagColor color);
  List<TaskDto> filter(TaskStatus status, UUID assigneeId, Tag tag);
}
