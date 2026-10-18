package org.example.taskmanager.taskmanager.infrastructure.adapter;

import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.entity.BoardColumn;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.BoardType;
import org.example.taskmanager.taskmanager.domain.port.BoardAccessPort;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.repository.BoardColumnRepository;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class BoardAccessAdapter implements BoardAccessPort {
  private final BoardColumnRepository boardColumnRepository;
  private final BoardRepository boardRepository;

  public BoardAccessAdapter(
          BoardColumnRepository boardColumnRepository,
          BoardRepository boardRepository
  ) {
    this.boardColumnRepository = boardColumnRepository;
    this.boardRepository = boardRepository;
  }

  /**
   * Проверка существования Board по указанному boardId
   * @param boardId Board ID (UUID)
   * @return true - Board существует, false - не существует
   */
  @Override
  public boolean exists(UUID boardId) {
    return boardRepository.existsById(boardId);
  }

  /**
   * Метод для проверки активности Board
   * @param boardId Board ID (UUID)
   * @return true - board активен, false - board не активен
   * @throws NotFoundException в случае несуществования Board по указанному ID
   */
  @Override
  public boolean isActive(UUID boardId) {
    return getBoard(boardId).getArchiveState() == ArchiveState.ACTIVE;
  }

  /**
   * Метод-фильтр, требующий, чтобы Board по указанному ID был активным
   * @param boardId board ID (UUID)
   * @throws NotFoundException в случае несуществования Board по указанному ID
   * @throws IllegalStateException в случае нахождения Board в состоянии Archived
   */
  @Override
  public void requireActive(UUID boardId, String operationName) {
    if (!isActive(boardId)) {
      throw new IllegalStateException("Cannot do " + operationName
              + " because board with id=" + boardId + " is archived");
    }
  }

  /**
   * Проверка на то, что указанная доска - KANBAN формата
   * @param boardId id of Board (UUID)
   * @return true - kanban, false - не kanban
   */
  @Override
  public boolean isBoardKanban(UUID boardId) {
    return getBoard(boardId).getType() == BoardType.KANBAN;
  }

  @Override
  public void requireKanban(UUID boardId, String operationName) {
    if (!isBoardKanban(boardId)) {
      throw new IllegalStateException("Cannot do " + operationName
              + " because board with id is not kanban type");
    }
  }

  /**
   * Проверка на то, что BoardColumn с указанным columnId является
   * частью Board с указанным BoardId
   * @param boardId id of Board (UUID)
   * @param columnId id of BoardColumn (UUID)
   */
  @Override
  public void requireColumnOfBoard(UUID boardId, UUID columnId) {
    BoardColumn column = getColumn(columnId);

    if (!Objects.equals(column.getBoardId(), boardId)) {
      throw new IllegalArgumentException("Column with id=" + columnId
              + " is not part of board with id=" + boardId);
    }
  }

  @Override
  public void validateTaskCreation(UUID boardId, UUID columnId) {

  }

  // ====== helpers =======

  private Board getBoard(UUID boardId) {
    return boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundException("Board not found, boardId=" + boardId));
  }

  private BoardColumn getColumn(UUID columnId) {
    return boardColumnRepository.findById(columnId)
            .orElseThrow(() -> new NotFoundException("Board column not found, id=" + columnId));
  }
}
