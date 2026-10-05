package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.response.BoardColumnDto;
import org.example.taskmanager.taskmanager.controller.dto.response.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TaskDto;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEvent;
import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.entity.BoardColumn;
import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.BoardType;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEventPublisher;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
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
import java.util.Comparator;
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

  private final DomainEventPublisher  domainEventPublisher;

  public  BoardServiceImpl(ProjectRepository projectRepository, TaskRepository taskRepository,
                           BoardColumnRepository boardColumnRepository, BoardRepository boardRepository,
                           BoardMapper boardMapper, BoardColumnMapper boardColumnMapper, TaskMapper taskMapper,
                           DomainEventPublisher domainEventPublisher) {
    this.projectRepository = projectRepository;
    this.taskRepository = taskRepository;
    this.boardRepository = boardRepository;
    this.boardColumnRepository = boardColumnRepository;
    this.boardMapper = boardMapper;
    this.boardColumnMapper = boardColumnMapper;
    this.taskMapper = taskMapper;

    this.domainEventPublisher = domainEventPublisher;
  }

  @Override
  public BoardDto create(UUID projectId, String name, String description, BoardType boardType) {
    if (!projectRepository.existsById(projectId))
      throw new NotFoundException("Project with id " + projectId + " not found during board create operation");

    Board board = new Board(projectId, name, description, boardType);

    boardRepository.save(board);

    return boardMapper.toDto(board);
  }

  @Override
  public BoardDto archive(UUID boardId, ArchiveReason reason) {
    Board board = getBoard(boardId, "archive");

    board.archive(reason);
    boardRepository.save(board);
    board.getEvents().forEach(domainEventPublisher::publish);
    board.clearEvents();

    return boardMapper.toDto(board);
  }

  @Override
  public List<BoardDto> archiveByProject(UUID projectId, ArchiveReason reason) {
    List<Board> boards = boardRepository.findBoardsByProjectId(projectId);
    checkBoardsFound(boards, projectId, "archiveByProject");

    for (Board board : boards) {
      if (board.getArchiveState() == ArchiveState.ACTIVE) {
        board.archive(reason);
        boardRepository.save(board);
        board.getEvents().forEach(domainEventPublisher::publish);
        board.clearEvents();
      }
    }

    return boards.stream().map(boardMapper::toDto).toList();
  }

  @Override
  public BoardDto unarchive(UUID boardId, UnarchiveReason reason) {
    Board board = getBoard(boardId, "unarchive");

    Project parent = getParent(board.getProjectId(), "unarchive");
    checkParentBeforeUnarchive(parent);

    board.unarchive(reason);
    boardRepository.save(board);
    board.getEvents().forEach(domainEventPublisher::publish);
    board.clearEvents();

    return boardMapper.toDto(board);
  }

  @Override
  public List<BoardDto> unarchiveByProject(UUID projectId, UnarchiveReason reason) {
    List<Board> boards = boardRepository.findBoardsByProjectId(projectId);

    checkBoardsFound(boards, projectId, "unarchiveByProject");

    Project parent = getParent(projectId, "unarchiveByProject");
    checkParentBeforeUnarchive(parent);

    for (Board board : boards) {
      if (board.canBeUnarchived(reason)) {
        board.unarchive(reason);
        boardRepository.save(board);
        board.getEvents().forEach(domainEventPublisher::publish);
        board.clearEvents();
      }
    }

    return boards.stream().map(boardMapper::toDto).toList();
  }

  @Override
  public List<TaskDto> getTasks(UUID boardId) {
    if (!boardRepository.existsById(boardId)) {
      throw new NotFoundException("Board with id " + boardId + " not found during getTasks operation");
    }

    List<Task> tasks = taskRepository.findAllByBoardId(boardId);
    if (tasks.isEmpty()) {
      throw new NotFoundException("Tasks not found with board as parent with id "
              + boardId + " during getTasks operation");
    }

    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Override
  public BoardColumnDto changeColumnName(UUID columnId, String newName) {
    BoardColumn boardColumn =  boardColumnRepository.findById(columnId)
            .orElseThrow(() -> new NotFoundException("Column with id "
                    + columnId + " not found during changeColumnName operation"));

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
      throw new NotFoundException("Board columns not found with board as parent with id "
              + boardId + "during changeColumnPosition operation");
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

    columns.sort(Comparator.comparingInt(BoardColumn::getPosition));

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

  private Board getBoard(UUID boardId, String operationName) {
    return boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundException("Board with id " + boardId + " not found during "
                    + operationName + " operation"));
  }

  private Project getParent(UUID projectId, String operationName) {
    return projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project with id " + projectId
                    + " as parent of board not found during " + operationName + " operation"));
  }

  private void checkParentBeforeUnarchive(Project parent) {
    if (parent.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalStateException("Cannot unarchive boards because project (parent) with id "
              + parent.getId() + " is archived");
  }

  private void checkBoardsFound(List<Board> boards, UUID projectId, String operationName) {
    if (boards.isEmpty())
      throw new NotFoundException("Boards with projectId " + projectId + " as parent id not found during "
              + operationName + " operation");
  }
}
