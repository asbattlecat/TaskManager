package org.example.taskmanager.taskmanager.infrastructure.adapter;

import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.port.BoardAccessPort;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BoardAccessAdapter implements BoardAccessPort {
  private final BoardRepository boardRepository;

  public BoardAccessAdapter(
          BoardRepository boardRepository
  ) {
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

  // ====== helpers =======

  private Board getBoard(UUID boardId) {
    return boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundException("Board not found, boardId=" + boardId));
  }
}
