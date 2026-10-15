package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.domain.entity.Task;

import java.util.UUID;

/**
 * Многофункциональный интерфейс для проверки доступа пользователей
 * к Workspace и сущностям более низкого уровня (Project, Board, Task).
 * Также проводится проверка пользователя на наличие активного статуса, плюс
 * проверяется активность Workspace/Project/Board/Task
 */
public interface WorkspaceAccessChecker {
  /**
   * Проверка прав доступа указанного WorkspaceMember к Workspace по boardId,
   * проверка пользователя на статус (должен быть активен)
   * @param boardId ID of Board (UUID)
   * @param workspaceMemberId ID of WorkspaceMember (UUID)
   */
  void check(UUID boardId, UUID workspaceMemberId);
  /**
   * Проверка прав доступа двоих указанных WorkspaceMember к Workspace по boardId,
   * проверка пользователя на статус (должен быть активен). Этот метод был
   * добавлен во благо уменьшения количества запросов к бд.
   * @param boardId ID of Board (UUID)
   * @param firstId ID of first WorkspaceMember (UUID)
   * @param secondId ID of second WorkspaceMember (uUID)
   */
  void checkTwo(UUID boardId, UUID firstId, UUID secondId);
}
