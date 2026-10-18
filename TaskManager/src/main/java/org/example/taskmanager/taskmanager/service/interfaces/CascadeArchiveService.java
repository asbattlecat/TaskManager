package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.domain.records.CascadeArchiveResult;
import org.example.taskmanager.taskmanager.domain.records.CascadeUnarchiveResult;

import java.util.UUID;

public interface CascadeArchiveService {
  /**
   * Метод для каскадной архивации Projects, Boards, Tasks, что принадлежат Workspace.
   * <p>Ставиться archiveState в позицию "PARENT_ARCHIVED". Архивируются только ACTIVE сущности.
   * <p>Вызывать только в методе WorkspaceService.archive() или другом методе, что
   * архивирует Workspace сущность, и при этом требуется каскадная архивация принадлежащих
   * к Workspace сущностей
   * @param workspaceId Workspace ID (UUID)
   * @param memberId WorkspaceMember ID (UUID)
   * @return Результат каскадной архивации
   */
  CascadeArchiveResult archiveByWorkspace(UUID workspaceId, UUID memberId);

  /**
   * Метод для каскадной архивации Boards, Tasks, что принадлежат Project.
   * <p>Ставиться archiveState в позицию "PARENT_ARCHIVED". Архивируются только ACTIVE сущности.
   * <p>Вызывать только в методе ProjectService.archive() или другом методе, что
   * архивирует Project сущность, и при этом требуется каскадная архивация принадлежащих
   * к Project сущностей
   * @param projectId Project ID (UUID)
   * @param memberId WorkspaceMember ID (UUID)
   * @return Результат каскадной архивации
   */
  CascadeArchiveResult archiveByProject(UUID projectId, UUID memberId);

  /**
   * Метод для каскадной архивации Tasks, что принадлежат Board.
   * <p>Ставиться archiveState в позицию "PARENT_ARCHIVED". Архивируются только ACTIVE сущности
   * <p>Вызывать только в методе BoardService.archive() или другом методе, что
   * архивирует Board сущность, и при этом требуется каскадная архивация принадлежащих
   * к Board сущностей
   * @param boardId Board ID (UUID)
   * @param memberId WorkspaceMember ID (UUID)
   * @return Результат каскадной архивации
   */
  CascadeArchiveResult archiveByBoard(UUID boardId, UUID memberId);

  /**
   * Метод для каскадной разархивации Projects, Boards, Tasks, что принадлежат Workspace.
   * <p>Ставиться archiveState в позицию "ACTIVE". Архивируются только PARENT_ARCHIVED сущности.
   * Но, при этом, если родитель этой сущности не находится в состоянии 'ACTIVE', то разархивация
   * не происходит.
   * <p>Вызывать только в методе WorkspaceService.unarchive() или другом методе, что
   * разархивирует Workspace сущность, и при этом требуется каскадная разархивация принадлежащих
   * к Workspace сущностей
   * @param workspaceId Workspace ID (UUID)
   * @param memberId WorkspaceMember ID (UUID)
   * @return Результат каскадной разархивации
   */
  CascadeUnarchiveResult unarchiveByWorkspace(UUID workspaceId, UUID memberId);

  /**
   * Метод для каскадной разархивации Boards, Tasks, что принадлежат Project.
   * <p>Ставиться archiveState в позицию "ACTIVE". Архивируются только PARENT_ARCHIVED сущности.
   * Но, при этом, если родитель этой сущности не находится в состоянии 'ACTIVE', то разархивация
   * не происходит.
   * <p>Вызывать только в методе ProjectService.unarchive() или другом методе, что
   * разархивирует Project сущность, и при этом требуется каскадная разархивация принадлежащих
   * к Project сущностей
   * @param projectId Project ID (UUID)
   * @param memberId WorkspaceMember ID (UUID)
   * @return Результат каскадной разархивации
   */
  CascadeUnarchiveResult unarchiveByProject(UUID projectId, UUID memberId);

  /**
   * Метод для каскадной разархивации Tasks, что принадлежат Board.
   * <p>Ставиться archiveState в позицию "ACTIVE". Архивируются только PARENT_ARCHIVED сущности.
   * Но, при этом, если родитель этой сущности не находится в состоянии 'ACTIVE', то разархивация
   * не происходит.
   * <p>Вызывать только в методе BoardService.unarchive() или другом методе, что
   * разархивирует Board сущность, и при этом требуется каскадная разархивация принадлежащих
   * к Board сущностей
   * @param boardId Project ID (UUID)
   * @param memberId WorkspaceMember ID (UUID)
   * @return Результат каскадной разархивации
   */
  CascadeUnarchiveResult unarchiveByBoard(UUID boardId, UUID memberId);
}
