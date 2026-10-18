package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.domain.records.CascadeArchiveResult;

import java.util.UUID;

public interface CascadeArchiveService {
  /**
   * Метод для каскадной архивации Projects, Boards, Tasks, что принадлежат Workspace.
   * Ставиться archiveState в позицию "PARENT_ARCHIVED". Архивируются только ACTIVE сущности.
   * Вызывать только в методе WorkspaceService.archive() или другом методе, что
   * архивирует Workspace сущность, и при этом требуется каскадная архивация принадлежащих
   * к Workspace сущностей
   * @param workspaceId Workspace ID (UUID)
   * @return Результат каскадной архивации
   */
  CascadeArchiveResult archiveByWorkspace(UUID workspaceId);
  /**
   * Метод для каскадной архивации Boards, Tasks, что принадлежат Project.
   * Ставиться archiveState в позицию "PARENT_ARCHIVED". Архивируются только ACTIVE сущности.
   * Вызывать только в методе ProjectService.archive() или другом методе, что
   * архивирует Project сущность, и при этом требуется каскадная архивация принадлежащих
   * к Project сущностей
   * @param projectId Project ID (UUID)
   * @return Результат каскадной архивации
   */
  CascadeArchiveResult archiveByProject(UUID projectId);
  /**
   * Метод для каскадной архивации Tasks, что принадлежат Board.
   * Ставиться archiveState в позицию "PARENT_ARCHIVED". Архивируются только ACTIVE сущности
   * Вызывать только в методе BoardService.archive() или другом методе, что
   * архивирует Board сущность, и при этом требуется каскадная архивация принадлежащих
   * к Board сущностей
   * @param boardId Board ID (UUID)
   * @return Результат каскадной архивации
   */
  CascadeArchiveResult archiveByBoard(UUID boardId);
}
