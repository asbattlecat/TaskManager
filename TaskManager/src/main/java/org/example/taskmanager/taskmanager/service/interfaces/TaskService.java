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
  TaskDto create(UUID boardId, UUID columnId, String name, String description, TaskStatus status,
                 TaskPriority priority, UUID assigneeId, UUID creatorId, Instant deadline);
  TaskDto archive(UUID taskId, ArchiveReason archiveReason);
  List<TaskDto> archiveByBoard(UUID boardId, ArchiveReason reason);
  TaskDto unarchive(UUID taskId, UnarchiveReason reason);
  List<TaskDto> unarchiveByBoard(UUID boardId, UnarchiveReason reason);
  TaskDto changeName(UUID taskId, String newName, UUID userId);
  TaskDto changeDescription(UUID taskId, String newDescription, UUID userId);
  TaskDto changePriority(UUID taskId, TaskPriority newPriority, UUID userId);
  TaskDto changeColumn(UUID taskId, UUID newColumnId);
  TaskDto changeDeadline(UUID taskId, Instant newDeadline, UUID userId);
  TaskDto changeStatus(UUID taskId, TaskStatus newStatus, UUID userId);
  TaskDto setAssignee(UUID taskId, UUID workspaceMemberId, UUID userId);
  CommentDto addComment(UUID taskId, UUID workspaceMemberId, String content);
  TagDto addTag(UUID taskId, String name, TagColor color);
  List<TaskDto> filter(UUID boardId, TaskStatus status, UUID assigneeId, Tag tag);
  List<AuditEntityDto> getTaskChangesHistory(UUID taskId);
}
