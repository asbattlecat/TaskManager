package org.example.taskmanager.taskmanager.domain.port;

import java.util.UUID;

/**
 * Исходящий порт: доступ Task-агрегата к сведениям о Board. Возможно и другое применение
 * Реализация расположена в инфраструктуре
 */
public interface BoardAccessPort {
  boolean exists(UUID boardId);
  boolean isActive(UUID boardId);
  void requireActive(UUID boardId, String operationName);
  boolean isBoardKanban(UUID boardId);
  void requireKanban(UUID boardId, String operationName);
  void requireColumnOfBoard(UUID boardId, UUID columnId);
}
