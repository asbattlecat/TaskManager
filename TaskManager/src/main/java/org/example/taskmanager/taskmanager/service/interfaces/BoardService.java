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
  BoardDto create(UUID projectId, String name, String description, BoardType boardType);
  BoardDto archive(UUID boardId, ArchiveReason archiveReason);
  BoardDto unarchive(UUID boardId, UnarchiveReason reason);
  List<TaskDto> getTasks(UUID projectId);
  BoardColumnDto changeColumnName(UUID columnId, String newName);
  List<BoardColumnDto> changeColumnPosition(UUID boardId, Integer oldPosition, Integer newPosition);
}
