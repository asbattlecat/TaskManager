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
import org.example.taskmanager.taskmanager.domain.records.CascadeArchiveResult;
import org.example.taskmanager.taskmanager.domain.records.CascadeUnarchiveResult;
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
import org.example.taskmanager.taskmanager.service.interfaces.CascadeArchiveService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class BoardServiceImpl implements BoardService {
  private final CascadeArchiveService cascadeArchiveService;
  private final BoardColumnRepository boardColumnRepository;
  private final DomainEventPublisher  domainEventPublisher;
  private final BoardColumnMapper boardColumnMapper;
  private final ProjectRepository projectRepository;
  private final BoardRepository boardRepository;
  private final TaskRepository taskRepository;
  private final BoardMapper boardMapper;
  private final TaskMapper taskMapper;


  public  BoardServiceImpl(
          CascadeArchiveService cascadeArchiveService,
          BoardColumnRepository boardColumnRepository,
          DomainEventPublisher domainEventPublisher,
          ProjectRepository projectRepository,
          BoardColumnMapper boardColumnMapper,
          BoardRepository boardRepository,
          TaskRepository taskRepository,
          BoardMapper boardMapper,
          TaskMapper taskMapper
  ) {
    this.cascadeArchiveService = cascadeArchiveService;
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
    log.info("creating board, projectId={}, name={}, description={}, boardType={}",
            projectId, name, description, boardType);

    if (!projectRepository.existsById(projectId)) {
      log.error("board not created, project not found, projectId={}", projectId);
      throw new NotFoundException("Project with id " + projectId + " not found during board create operation");
    }

    Board board = new Board(projectId, name, description, boardType);
    boardRepository.save(board);

    log.info("board created and saved in database, boardId={}", board.getId());
    return boardMapper.toDto(board);
  }

  @Transactional
  @Override
  public BoardDto archive(UUID boardId, ArchiveReason reason) {
    log.info("board archive operation starts, boardId={}, reason={}", boardId, reason);
    Board board = getBoard(boardId, "archive");

    board.archive(reason);
    boardRepository.save(board);
    CascadeArchiveResult result = cascadeArchiveService.archiveByBoard(boardId);

    log.debug("board archive completed, boardId={}, cascaded projects={}, boards={}, tasks={}",
            boardId,
            result.projectsArchived(),
            result.boardsArchived(),
            result.tasksArchived()
    );
    return boardMapper.toDto(board);
  }

  @Transactional
  @Override
  public BoardDto unarchive(UUID boardId, UnarchiveReason reason) {
    log.info("board unarchive operation starts, boardId={}, reason={}", boardId, reason);
    Board board = getBoard(boardId, "unarchive");

    Project parent = getParent(board.getProjectId(), "unarchive");
    checkParentBeforeUnarchive(parent);

    board.unarchive(reason);
    boardRepository.save(board);
    CascadeUnarchiveResult result = cascadeArchiveService.unarchiveByBoard(boardId);

    log.debug("board unarchive completed, boardId={}, cascaded projects={}, boards={}, tasks={}",
            boardId,
            result.projectsUnarchived(),
            result.projectsUnarchived(),
            result.projectsUnarchived()
    );
    return boardMapper.toDto(board);
  }

  @Transactional
  @Override
  public List<TaskDto> getTasks(UUID boardId) {
    Board board = getBoard(boardId, "getTasks");
    isBoardActive(board, "getTasks");

    List<Task> tasks = taskRepository.findAllByBoardId(boardId);

    log.debug("board getTasks operation completed, boardId={}", boardId);
    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public BoardColumnDto changeColumnName(UUID columnId, String newName) {
    log.info("board changeColumnName operation starts, columnId={}, newName={}", columnId, newName);

    BoardColumn boardColumn =  boardColumnRepository
            .findById(columnId)
            .orElseThrow(() -> {
              log.error("board column not found, columnId={}", columnId);
              return new NotFoundException("Column with id "
                      + columnId + " not found during changeColumnName operation");
            });

    boardColumn.setName(newName);

    boardColumnRepository.save(boardColumn);

    log.debug("board changeColumnName operation completed, columnId={}, newName={}", columnId, newName);
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
   * @return List of BoardColumnDto - отсортированные колонки с обновленные порядком
   */
  @Transactional
  @Override
  public List<BoardColumnDto> changeColumnPosition(UUID boardId, Integer oldPosition, Integer newPosition) {
    log.info("board changeColumnPosition operation starts, boardId={}, oldPosition={}, newPosition={}",
            boardId, oldPosition, newPosition);
    List<BoardColumn> columns = boardColumnRepository.findAllByBoardIdOrderByPositionAscPessLock(boardId);

    if (columns.isEmpty()) {
      log.error("board columns not found, boardId={}", boardId);
      throw new NotFoundException("Board columns not found with board as parent with id "
              + boardId + "during changeColumnPosition operation");
    }

    int listSize = columns.size();
    if (oldPosition >= listSize) {
      log.error("old position out of range, oldPosition={}, listSize={}", oldPosition, listSize);
      throw new ArrayIndexOutOfBoundsException("Old position out of range");
    } else if (newPosition >= listSize) {
      log.error("new position out of range, newPosition={}, listSize={}", newPosition, listSize);
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

    log.debug("board changeColumnPosition operation completed, boardId={}, oldPosition={}, newPosition={}",
            boardId, oldPosition, newPosition);
    return columns.stream().map(boardColumnMapper::toDto).toList();
  }

  private Board getBoard(UUID boardId, String operationName) {
    return boardRepository
            .findById(boardId)
            .orElseThrow(() -> {
              log.error("board not found, boardId={}, operationName={}", boardId, operationName);
              return new NotFoundException("Board with id " + boardId + " not found during "
                      + operationName + " operation");
            });
  }

  private Project getParent(UUID projectId, String operationName) {
    return projectRepository
            .findById(projectId)
            .orElseThrow(() -> {
              log.error("project not found, projectId={}, operationName={}", projectId, operationName);
              return new NotFoundException("Project with id " + projectId
                      + " as parent of board not found during " + operationName + " operation");
            });
  }

  private void checkParentBeforeUnarchive(Project parent) {
    if (parent.getArchiveState() != ArchiveState.ACTIVE) {
      log.error("cannot unarchive board, project (parent) is archived, parentId={}", parent.getId());
      throw new IllegalStateException("Cannot unarchive boards because project (parent) with id "
              + parent.getId() + " is archived");
    }
  }

  private void isBoardActive(Board board, String operationName) {
    if (board.getArchiveState() != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Cannot do " + operationName
              + " operation because board with id " + board.getId() + " is archived");
    }
  }
}
