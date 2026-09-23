package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.BoardColumnDto;
import org.example.taskmanager.taskmanager.controller.dto.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.TaskDto;
import org.example.taskmanager.taskmanager.domain.enums.BoardType;

import java.util.List;
import java.util.UUID;

public interface BoardService {
  BoardDto create(UUID projectId, String name, String description, BoardType boardType);
  List<TaskDto> getTasks(UUID projectId);
  BoardColumnDto changeColumnName(UUID columnId, String newName);
  List<BoardColumnDto> changeColumnPosition(UUID boardId, Integer oldPosition, Integer newPosition);
}
