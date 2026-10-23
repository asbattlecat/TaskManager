package org.example.taskmanager.taskmanager.infrastructure.adapter;

import org.example.taskmanager.taskmanager.domain.aggregate.Project;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.port.ProjectAccessPort;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProjectAccessAdapter implements ProjectAccessPort {
  private final ProjectRepository projectRepository;

  public ProjectAccessAdapter(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  /**
   * Метод для проверки существования Project
   * @param projectId project ID (UUID)
   * @return true - project существует, false - project не существует
   */
  @Override
  public boolean exists(UUID projectId) {
    return projectRepository.existsById(projectId);
  }

  /**
   * Метод для проверки активности Project
   * @param projectId project ID (UUID)
   * @return true - project активен, false - project не активен
   * @throws NotFoundException в случае несуществования Project по указанному ID
   */
  @Override
  public boolean isActive(UUID projectId) {
    return getProject(projectId).getArchiveState() == ArchiveState.ACTIVE;
  }

  /**
   * Метод-фильтр, требующий, чтобы Project по указанному ID был активным
   * @param projectId project ID (UUID)
   * @throws NotFoundException в случае несуществования Project по указанному ID
   * @throws IllegalStateException в случае нахождения Project в состоянии Archived
   */
  @Override
  public void requireActive(UUID projectId, String operationName) {
    if (!isActive(projectId)) {
      throw new IllegalStateException("Cannot do " + operationName
              + " because project with id=" + projectId + " is archived");
    }
  }

  // ====== helpers =======

  private Project getProject(UUID projectId) {
    return projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found, projectId=" + projectId));
  }
}
