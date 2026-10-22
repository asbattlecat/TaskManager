package org.example.taskmanager.taskmanager.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.BoardColumnDto;
import org.example.taskmanager.taskmanager.controller.dto.response.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TaskDto;
import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.entity.BoardColumn;
import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.example.taskmanager.taskmanager.domain.enums.*;
import org.example.taskmanager.taskmanager.domain.port.ProjectAccessPort;
import org.example.taskmanager.taskmanager.domain.records.CascadeArchiveResult;
import org.example.taskmanager.taskmanager.domain.records.CascadeUnarchiveResult;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.BoardColumnMapper;
import org.example.taskmanager.taskmanager.mapper.BoardMapper;
import org.example.taskmanager.taskmanager.mapper.TaskMapper;
import org.example.taskmanager.taskmanager.repository.BoardColumnRepository;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.TaskRepository;
import org.example.taskmanager.taskmanager.service.interfaces.BoardService;
import org.example.taskmanager.taskmanager.service.interfaces.CascadeArchiveService;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceAccessChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@Validated
public class BoardServiceImpl implements BoardService {
  private final WorkspaceAccessChecker workspaceAccessChecker;
  private final CascadeArchiveService cascadeArchiveService;
  private final BoardColumnRepository boardColumnRepository;
  private final BoardColumnMapper boardColumnMapper;
  private final ProjectAccessPort projectAccessPort;
  private final BoardRepository boardRepository;
  private final TaskRepository taskRepository;
  private final BoardMapper boardMapper;
  private final TaskMapper taskMapper;


  public  BoardServiceImpl(
          WorkspaceAccessChecker workspaceAccessChecker,
          CascadeArchiveService cascadeArchiveService,
          BoardColumnRepository boardColumnRepository,
          ProjectAccessPort projectAccessPort,
          BoardColumnMapper boardColumnMapper,
          BoardRepository boardRepository,
          TaskRepository taskRepository,
          BoardMapper boardMapper,
          TaskMapper taskMapper
  ) {
    this.workspaceAccessChecker = workspaceAccessChecker;
    this.cascadeArchiveService = cascadeArchiveService;
    this.boardColumnRepository = boardColumnRepository;
    this.projectAccessPort = projectAccessPort;
    this.boardColumnMapper = boardColumnMapper;
    this.boardRepository = boardRepository;
    this.taskRepository = taskRepository;
    this.boardMapper = boardMapper;
    this.taskMapper = taskMapper;
  }

  @Transactional
  @Override
  public BoardDto create(
          @NotNull UUID projectId,
          @NotBlank String name,
          String description,
          @NotNull BoardType boardType,
          @NotNull UUID memberId
  ) {
    log.info("creating board, projectId={}, name={}, description={}, boardType={}",
            projectId, name, description, boardType);

    workspaceAccessChecker.check(RootEntityType.PROJECT, projectId, memberId);
    projectAccessPort.requireActive(projectId, "create");

    Board board = new Board(projectId, name, description, boardType);
    boardRepository.save(board);

    log.info("BoardService created and saved in database, boardId={}", board.getId());
    return boardMapper.toDto(board);
  }

  // ================== архивация / разархивация ==================

  @Transactional
  @Override
  public BoardDto archive(
          @NotNull UUID boardId,
          @NotNull UUID memberId
  ) {
    log.info("board archive starts, boardId={}", boardId);
    workspaceAccessChecker.check(RootEntityType.BOARD, boardId, memberId);

    Board board = getBoard(boardId, "archive");

    board.archive(ArchiveReason.USER_ACTION);
    boardRepository.save(board);
    CascadeArchiveResult result = cascadeArchiveService.archiveByBoard(boardId, memberId);

    log.debug("BoardService archive completed, boardId={}, cascaded projects={}, boards={}, tasks={}",
            boardId,
            result.projectsArchived(),
            result.boardsArchived(),
            result.tasksArchived()
    );
    return boardMapper.toDto(board);
  }

  @Transactional
  @Override
  public BoardDto unarchive(
          @NotNull UUID boardId,
          @NotNull UUID memberId
  ) {
    log.info("board unarchive starts, boardId={}", boardId);
    workspaceAccessChecker.check(RootEntityType.BOARD, boardId, memberId);

    Board board = getBoard(boardId, "unarchive");

    board.unarchive(UnarchiveReason.USER_ACTION);
    boardRepository.save(board);
    CascadeUnarchiveResult result = cascadeArchiveService.unarchiveByBoard(boardId, memberId);

    log.debug("BoardService unarchive completed, boardId={}, cascaded projects={}, boards={}, tasks={}",
            boardId,
            result.projectsUnarchived(),
            result.projectsUnarchived(),
            result.projectsUnarchived()
    );
    return boardMapper.toDto(board);
  }

  // ================== архивация / разархивация ==================

  @Transactional
  @Override
  public BoardDto changeName(
          @NotNull UUID boardId,
          @NotBlank String newName,
          @NotNull UUID memberId
  ) {
    workspaceAccessChecker.check(RootEntityType.BOARD, boardId, memberId);
    Board board = getBoard(boardId, "changeName");

    // проверка активности борда будет внутри агрегата
    board.changeName(newName);

    log.debug("BoardService changeName completed, boardId={}", boardId);
    return boardMapper.toDto(board);
  }

  @Transactional
  @Override
  public BoardDto changeDescription(
          @NotNull UUID boardId,
          String newDescription,
          @NotNull UUID memberId
  ) {
    workspaceAccessChecker.check(RootEntityType.BOARD, boardId, memberId);
    Board board = getBoard(boardId, "changeDescription");

    // проверка активности борда будет внутри агрегата
    board.changeDescription(newDescription);

    log.debug("BoardService changeDescription completed, boardId={}", boardId);
    return boardMapper.toDto(board);
  }

  // TODO добавить пагинацию?
  @Transactional(readOnly = true)
  @Override
  public List<TaskDto> getTasks(
          @NotNull UUID boardId,
          @NotNull UUID memberId
  ) {
    workspaceAccessChecker.check(RootEntityType.BOARD, boardId, memberId);
    Board board = getBoard(boardId, "getTasks");
    requireBoardActive(board, "getTasks");

    List<Task> tasks = taskRepository.findAllByBoardId(boardId);

    log.debug("BoardService getTasks operation completed, boardId={}", boardId);
    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public BoardColumnDto changeColumnName(
          @NotNull UUID boardId,
          @NotNull UUID columnId,
          @NotBlank String newName,
          @NotNull UUID memberId
  ) {
    workspaceAccessChecker.check(RootEntityType.BOARD, boardId, memberId);
    Board board = getBoard(boardId, "changeColumnName");
    requireBoardActive(board, "changeColumnName");
    BoardColumn boardColumn =  boardColumnRepository
            .findById(columnId)
            .orElseThrow(() -> new NotFoundException("Board column not found, id=" + columnId));

    if (!Objects.equals(boardColumn.getBoardId(), boardId)) {
      throw new IllegalArgumentException("Board column does not belong to board, columnId="
              + columnId + ", boardId=" + boardId);
    }

    boardColumn.changeName(newName);

    log.debug("BoardService changeColumnName completed, columnId={}, newName={}", columnId, newName);
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
  public List<BoardColumnDto> changeColumnPosition(
          @NotNull UUID boardId,
          @NotNull Integer oldPosition,
          @NotNull Integer newPosition,
          @NotNull UUID memberId
  ) {
    workspaceAccessChecker.check(RootEntityType.BOARD, boardId, memberId);
    Board board = getBoard(boardId, "changeColumnPosition");
    requireBoardActive(board, "changeColumnPosition");

    List<BoardColumn> columns = boardColumnRepository.findAllByBoardIdOrderByPositionAscPessLock(boardId);

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

    if (oldPosition < newPosition) {
      columns.get(oldPosition).changePosition(newPosition);
      for (int i = oldPosition + 1; i <= newPosition; i++) {
        columns.get(i).changePosition(i - 1);
      }
    } else {
      columns.get(oldPosition).changePosition(newPosition);
      for (int i = oldPosition - 1; i >= newPosition; i--) {
        columns.get(i).changePosition(i + 1);
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

    log.debug("BoardService changeColumnPosition operation completed, boardId={}, oldPosition={}, newPosition={}",
            boardId, oldPosition, newPosition);
    return columns.stream().map(boardColumnMapper::toDto).toList();
  }

  // ===================== helpers =====================

  private Board getBoard(UUID boardId, String operationName) {
    return boardRepository
            .findById(boardId)
            .orElseThrow(() -> new NotFoundException("Board with id " + boardId
                    + " not found during " + operationName + " operation"));
  }

  private void requireBoardActive(Board board, String operationName) {
    if (board.getArchiveState() != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Cannot do " + operationName
              + " operation because board with id " + board.getId() + " is archived");
    }
  }
}
