package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.response.AuditEntityDto;
import org.example.taskmanager.taskmanager.controller.dto.response.CommentDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TagDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TaskDto;
import org.example.taskmanager.taskmanager.domain.entity.Tag;
import org.example.taskmanager.taskmanager.domain.enums.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaskService {
  TaskDto create(UUID boardId, UUID columnId, String name, String description,
                 TaskPriority priority, UUID assigneeId, UUID creatorId, Instant deadline);

  /**
   * Архивация Task сущности. Этот метод предназначен для ручной архивации сущности
   * (ArchiveReason == USER_ACTION)
   * @param taskId Task ID (UUID)
   * @param memberId WorkspaceMember ID (UUID) - тот, кто архивирует
   * @return TaskDTO - результат работы
   */
  TaskDto archive(UUID taskId, UUID memberId);
  /**
   * Разархивация Task сущности. Этот метод предназначен для ручной разархивации сущности
   * (UnarchiveReason == USER_ACTION)
   * @param taskId Task ID (UUID)
   * @param memberId WorkspaceMember ID (UUID) - тот, кто архивирует
   * @return TaskDTO - результат работы
   */
  TaskDto unarchive(UUID taskId, UUID memberId);
  TaskDto changeName(UUID taskId, String newName, UUID memberId);
  TaskDto changeDescription(UUID taskId, String newDescription, UUID memberId);
  TaskDto changePriority(UUID taskId, TaskPriority newPriority, UUID memberId);
  TaskDto changeColumnId(UUID taskId, UUID newColumnId, UUID memberId);
  TaskDto changeDeadline(UUID taskId, Instant newDeadline, UUID memberId);
  TaskDto changeStatus(UUID taskId, TaskStatus newStatus, UUID memberId);
  TaskDto changeAssigneeId(UUID taskId, UUID workspaceMemberId, UUID memberId);
  CommentDto addComment(UUID taskId, UUID workspaceMemberId, String content);
  TagDto addTag(UUID taskId, String name, TagColor color, UUID memberId);
  List<TaskDto> filter(UUID boardId, TaskStatus status, UUID assigneeId, String tagName,
                       UUID workspaceMemberId);
  List<AuditEntityDto> getTaskChangesHistory(UUID taskId, UUID memberId);
}
