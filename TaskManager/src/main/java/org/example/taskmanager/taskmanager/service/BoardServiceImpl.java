package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.BoardColumnDto;
import org.example.taskmanager.taskmanager.controller.dto.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.TaskDto;
import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.entity.BoardColumn;
import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.example.taskmanager.taskmanager.domain.enums.BoardType;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.BoardColumnMapper;
import org.example.taskmanager.taskmanager.mapper.BoardMapper;
import org.example.taskmanager.taskmanager.mapper.TaskMapper;
import org.example.taskmanager.taskmanager.repository.BoardColumnRepository;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.TaskRepository;
import org.example.taskmanager.taskmanager.service.interfaces.BoardService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BoardServiceImpl implements BoardService {
  private final ProjectRepository projectRepository;
  private final BoardRepository boardRepository;
  private final BoardColumnRepository boardColumnRepository;
  private final TaskRepository taskRepository;
  private final BoardMapper boardMapper;
  private final BoardColumnMapper boardColumnMapper;
  private final TaskMapper taskMapper;

  public  BoardServiceImpl(ProjectRepository projectRepository, TaskRepository taskRepository,
                           BoardColumnRepository boardColumnRepository, BoardRepository boardRepository,
                           BoardMapper boardMapper, BoardColumnMapper boardColumnMapper, TaskMapper taskMapper) {
    this.projectRepository = projectRepository;
    this.taskRepository = taskRepository;
    this.boardRepository = boardRepository;
    this.boardColumnRepository = boardColumnRepository;
    this.boardMapper = boardMapper;
    this.boardColumnMapper = boardColumnMapper;
    this.taskMapper = taskMapper;
  }

  @Override
  public BoardDto create(UUID projectId, String name, String description, BoardType boardType) {
    if (!projectRepository.existsById(projectId)) {
      throw new NotFoundException("Project not found");
    }

    Board board = new Board(projectId, name, description, boardType);

    return boardMapper.toDto(board);
  }

  @Override
  public List<TaskDto> getTasks(UUID boardId) {
    if (!boardRepository.existsById(boardId)) {
      throw new NotFoundException("Board not found");
    }

    List<Task> tasks = taskRepository.findAllByBoardId(boardId);
    if (tasks.isEmpty()) {
      throw new NotFoundException("Tasks not found");
    }

    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Override
  public BoardColumnDto changeColumnName(UUID columnId, String newName) {
    BoardColumn boardColumn =  boardColumnRepository.findById(columnId)
        .orElseThrow(() -> new NotFoundException("Column not found"));

    boardColumn.setName(newName);

    boardColumnRepository.save(boardColumn);

    return boardColumnMapper.toDto(boardColumn);
  }

  /**
   * Метод для перемещения колонки на новое место. В зависимости от положения oldPosition и newPosition
   * действуем по-разному.
   * Если (oldPosition < newPosition) - меняем позицию, и (oldPosition <= position < newPosition) - сдвигаем назад
   * на 1 значение.
   * Если (oldPosition > newPosition), то меняем позицию, и (newPosition < position <= oldPosition) - сдвигаем вперед
   * на 1 позицию
   * @param boardId айдишник борда
   * @param oldPosition старая позиция элемента, которую нужно заменить на новую
   * @param newPosition новая позиция
   * @return
   */
  @Override
  public List<BoardColumnDto> changeColumnPosition(UUID boardId, Integer oldPosition, Integer newPosition) {
    ArrayList<BoardColumn> columns = boardColumnRepository.findAllByBoardIdOrderByPositionAsc(boardId);

    if (columns.isEmpty()) {
      throw new NotFoundException("Board columns not found");
    }

    int listSize = columns.size();
    if (oldPosition >= listSize) {
      throw new ArrayIndexOutOfBoundsException("Old position out of range");
    } else if (newPosition >= listSize) {
      throw new ArrayIndexOutOfBoundsException("New position out of range");
    }

    if (oldPosition.equals(newPosition)) {
      throw new IllegalArgumentException("oldPosition is equal to newPosition");
    }

    // ПРОВЕРИТЬ на свежую голову
    // ПРОВЕРИТЬ на свежую голову
    // ПРОВЕРИТЬ на свежую голову
    // ПРОВЕРИТЬ на свежую голову
    if (oldPosition < newPosition) {
      columns.get(oldPosition).setPosition(newPosition);
      for (int i = oldPosition + 1; i <= newPosition; i++) {
        columns.get(i).setPosition(i - 1);
      }
    } else {
      columns.get(oldPosition).setPosition(newPosition);
      for (int i = oldPosition - 1; i >= newPosition; i--) {
        columns.get(i).setPosition(i + 1);
      }
    }

    columns.sort((first, second) -> Integer.compare(first.getPosition(), second.getPosition()));

    if (oldPosition < newPosition) {
      for (int i = oldPosition; i <= newPosition; i++) {
        boardColumnRepository.save(columns.get(i));
      }
    } else {
      for (int i = oldPosition; i >= newPosition; i--) {
        boardColumnRepository.save(columns.get(i));
      }
    }
    // ПРОВЕРИТЬ на свежую голову
    // ПРОВЕРИТЬ на свежую голову
    // ПРОВЕРИТЬ на свежую голову
    // ПРОВЕРИТЬ на свежую голову

    return columns.stream().map(boardColumnMapper::toDto).toList();
  }
}
