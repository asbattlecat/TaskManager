package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.BoardColumnDto;
import org.example.taskmanager.taskmanager.controller.dto.response.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TaskDto;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.entity.BoardColumn;
import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.BoardType;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEventPublisher;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.logs.CustomLogger;
import org.example.taskmanager.taskmanager.mapper.BoardColumnMapper;
import org.example.taskmanager.taskmanager.mapper.BoardMapper;
import org.example.taskmanager.taskmanager.mapper.TaskMapper;
import org.example.taskmanager.taskmanager.repository.BoardColumnRepository;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.TaskRepository;
import org.example.taskmanager.taskmanager.service.interfaces.BoardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class BoardServiceImpl implements BoardService {
  private final BoardColumnRepository boardColumnRepository;
  private final DomainEventPublisher  domainEventPublisher;
  private final BoardColumnMapper boardColumnMapper;
  private final ProjectRepository projectRepository;
  private final BoardRepository boardRepository;
  private final TaskRepository taskRepository;
  private final BoardMapper boardMapper;
  private final TaskMapper taskMapper;


  public  BoardServiceImpl(
          BoardColumnRepository boardColumnRepository,
          DomainEventPublisher domainEventPublisher,
          ProjectRepository projectRepository,
          BoardColumnMapper boardColumnMapper,
          BoardRepository boardRepository,
          TaskRepository taskRepository,
          BoardMapper boardMapper,
          TaskMapper taskMapper
  ) {
    this.boardColumnRepository = boardColumnRepository;
    this.domainEventPublisher = domainEventPublisher;
    this.projectRepository = projectRepository;
    this.boardColumnMapper = boardColumnMapper;
    this.boardRepository = boardRepository;
    this.taskRepository = taskRepository;
    this.boardMapper = boardMapper;
    this.taskMapper = taskMapper;
  }

  @Transactional
  @Override
  public BoardDto create(UUID projectId, String name, String description, BoardType boardType) {
    log.info("creating board, projectId={}, name={}, description-{}, boardType={}",
            projectId, name, description, boardType);

    if (!projectRepository.existsById(projectId))
      throw new NotFoundException("Project with id " + projectId + " not found during board create operation");

    Board board = new Board(projectId, name, description, boardType);

    boardRepository.save(board);

    log.info("board created and saved to database, id={}, projectId={}, name={}, description-{}, boardType={}",
            board.getId(), projectId, name, description, boardType);
    return boardMapper.toDto(board);
  }

  @Transactional
  @Override
  public BoardDto archive(UUID boardId, ArchiveReason reason) {
    log.info("board archive starts, id={}, reason={}", boardId, reason);
    Board board = getBoard(boardId, "archive");

    board.archive(reason);
    boardRepository.save(board);
    board.getEvents().forEach(domainEventPublisher::publish);
    board.clearEvents();

    log.debug("board archive completed, id={}, reason={}", boardId, reason);
    return boardMapper.toDto(board);
  }

  @Transactional
  @Override
  public List<BoardDto> archiveByProject(UUID projectId, ArchiveReason reason) {
    log.info("board archiveByProject starts, projectId={}, reason={}", projectId, reason);

    List<Board> boards = boardRepository
            .findBoardsByProjectIdWithPessimisticWriteLock(projectId);

    for (Board board : boards) {
      if (board.getArchiveState() == ArchiveState.ACTIVE) {
        board.archive(reason);
        boardRepository.save(board);
        board.getEvents().forEach(domainEventPublisher::publish);
        board.clearEvents();
      }
    }

    log.debug("board archiveByProject completed, projectId={}, reason={}", projectId, reason);
    return boards.stream().map(boardMapper::toDto).toList();
  }

  @Transactional
  @Override
  public BoardDto unarchive(UUID boardId, UnarchiveReason reason) {
    log.info("board unarchive starts, id={}, reason={}", boardId, reason);
    Board board = getBoard(boardId, "unarchive");

    Project parent = getParent(board.getProjectId(), "unarchive");
    checkParentBeforeUnarchive(parent);

    board.unarchive(reason);
    boardRepository.save(board);
    board.getEvents().forEach(domainEventPublisher::publish);
    board.clearEvents();

    log.debug("board unarchive completed, id={}, reason={}", boardId, reason);
    return boardMapper.toDto(board);
  }

  @Transactional
  @Override
  public List<BoardDto> unarchiveByProject(UUID projectId, UnarchiveReason reason) {
    log.info("board unarchiveByProject starts, projectId={}, reason={}", projectId, reason);
    List<Board> boards = boardRepository
            .findBoardsByProjectIdWithPessimisticWriteLock(projectId);

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

    log.debug("board unarchiveByProject completed, projectId={}, reason={}", projectId, reason);
    return boards.stream().map(boardMapper::toDto).toList();
  }

  @Transactional
  @Override
  public List<TaskDto> getTasks(UUID boardId) {
    CustomLogger.operationStarts("board", "getTasks", "boardId", boardId);
    Board board = getBoard(boardId, "getTasks");

    isBoardActive(board, "getTasks");

    List<Task> tasks = taskRepository.findAllByBoardId(boardId);

    CustomLogger.operationCompleted("board", "getTasks", "boardId", boardId);
    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public BoardColumnDto changeColumnName(UUID columnId, String newName) {
    CustomLogger.operationStarts("board", "changeColumnName", "columnId", columnId);

    BoardColumn boardColumn =  boardColumnRepository.findById(columnId)
            .orElseThrow(() -> {
              log.error("board column not found, columnId={}", columnId);
              return new NotFoundException("Column with id "
                      + columnId + " not found during changeColumnName operation");
            });

    boardColumn.setName(newName);

    boardColumnRepository.save(boardColumn);

    CustomLogger.operationCompleted("board", "changeColumnName", "columnId", columnId);
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
  @Transactional
  @Override
  public List<BoardColumnDto> changeColumnPosition(UUID boardId, Integer oldPosition, Integer newPosition) {
    CustomLogger.operationStarts("board", "changeColumnPosition", "boardId", boardId);
    List<BoardColumn> columns = boardColumnRepository.findAllByBoardIdOrderByPositionAsc(boardId);

    if (columns.isEmpty()) {
      log.error("board columns not found, boardId={}", boardId);
      throw new NotFoundException("Board columns not found with board as parent with id "
              + boardId + "during changeColumnPosition operation");
    }

    int listSize = columns.size();
    if (oldPosition >= listSize) {
      log.error("Old position out of range, oldPosition={}, listSize={}", oldPosition, listSize);
      throw new ArrayIndexOutOfBoundsException("Old position out of range");
    } else if (newPosition >= listSize) {
      log.error("New position out of range, newPosition={}, listSize={}", newPosition, listSize);
      throw new ArrayIndexOutOfBoundsException("New position out of range");
    }

    if (oldPosition.equals(newPosition)) {
      log.error("oldPosition is equal to newPosition, position={}", oldPosition);
      throw new IllegalArgumentException("oldPosition is equal to newPosition");
    }

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

    CustomLogger.operationCompleted("board", "changeColumnPosition", "boardId", boardId);
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

  private void isBoardActive(Board board, String operationName) {
    if (board.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalStateException("Cannot do " + operationName
              + " operation because board with id " + board.getId() + " is archived");
  }
}
