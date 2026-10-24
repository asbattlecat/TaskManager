package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.response.BoardColumnDto;
import org.example.taskmanager.taskmanager.controller.dto.response.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TaskDto;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.BoardType;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;

import java.util.List;
import java.util.UUID;

public interface BoardService {
  BoardDto create(
          UUID projectId,
          String name,
          String description,
          BoardType boardType,
          UUID memberId
  );

  /**
   * Этот метод создан для следующего использования:
   * <p> Пользователь вручную архивирует Board. Вследствие этого с помощью bulk операций
   * каскадно архивируются принадлежащие этому Board Tasks.
   * @param boardId ID of Board (UUID)
   * @param memberId ID of WorkspaceMember (UUID)
   * @return BoardDto с измененными полями
   */
  BoardDto archive(UUID boardId, UUID memberId);

  /**
   * Этот метод создан для следующего использования:
   * <p> Пользователь вручную разархивирует Board. Вследствие этого с помощью bulk операций
   * каскадно разархивируются принадлежащие этому Board Tasks.
   * Условие успешного ручного разархивироваия:
   * <p>Родительские Project и Workspace должны быть активными
   * @param boardId ID of Board (UUID)
   * @param memberId ID of WorkspaceMember (UUID)
   * @return BoardDto с измененными полями
   */
  BoardDto unarchive(UUID boardId, UUID memberId);
  BoardDto changeName(UUID boardId, String newName, UUID memberId);
  BoardDto changeDescription(UUID boardId, String newDescription, UUID memberId);
  List<TaskDto> getTasks(UUID projectId, UUID memberid);
  BoardColumnDto changeColumnName(UUID boardId, UUID columnId, String newName, UUID memberId);
  List<BoardColumnDto> changeColumnPosition(UUID boardId, Integer oldPosition, Integer newPosition, UUID memberId);
}
