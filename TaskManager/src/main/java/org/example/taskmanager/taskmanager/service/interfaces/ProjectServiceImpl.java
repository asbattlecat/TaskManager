package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.ProjectDto;
import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.BoardMapper;
import org.example.taskmanager.taskmanager.mapper.ProjectMapper;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProjectServiceImpl implements ProjectService {
  private final WorkspaceRepository  workspaceRepository;
  private final ProjectRepository projectRepository;
  private final BoardRepository boardRepository;
  private final ProjectMapper projectMapper;
  private final BoardMapper boardMapper;

  public ProjectServiceImpl(WorkspaceRepository workspaceRepository, ProjectRepository projectRepository,
                            BoardRepository boardRepository, ProjectMapper projectMapper,
                            BoardMapper boardMapper) {
    this.workspaceRepository = workspaceRepository;
    this.projectRepository = projectRepository;
    this.boardRepository = boardRepository;
    this.projectMapper = projectMapper;
    this.boardMapper = boardMapper;
  }


  @Override
  public ProjectDto create(UUID workspaceId, String name, String description) {
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new NotFoundException("Workspace not found");
    }

    Project project = new Project(workspaceId, name, description);
    projectRepository.save(project);

    return projectMapper.toDto(project);
  }

  @Override
  public ProjectDto delete(UUID projectId) {
    Optional<Project> projectOptional = projectRepository.findById(projectId);
    if (projectOptional.isEmpty()) {
      throw new NotFoundException("Project not found");
    }

    projectRepository.delete(projectOptional.get());

    return projectMapper.toDto(projectOptional.get());
  }

  @Override
  public ProjectDto archive(UUID projectId) {
    Optional<Project> projectOptional = projectRepository.findById(projectId);
    if (projectOptional.isEmpty()) {
      throw new NotFoundException("Project not found");
    }

    Project project = projectOptional.get();
    project.setArchived(true);
    projectRepository.save(project);

    return projectMapper.toDto(project);
  }

  @Override
  public ProjectDto unarchive(UUID projectId) {
    Optional<Project> projectOptional = projectRepository.findById(projectId);
    if (projectOptional.isEmpty()) {
      throw new NotFoundException("Project not found");
    }

    Project project = projectOptional.get();
    project.setArchived(false);
    projectRepository.save(project);

    return projectMapper.toDto(project);
  }

  @Override
  public List<BoardDto> getProjectBoards(UUID projectId) {
    if (!projectRepository.existsById(projectId)) {
      throw new NotFoundException("Project not found");
    }

    List<Board> boards = boardRepository.getBoardsByProjectId(projectId);

    return boards.stream().map(boardMapper::toDto).toList();
  }

  @Override
  public ProjectDto changeName(UUID projectId, String newName) {
    Optional<Project> projectOptional = projectRepository.findById(projectId);
    if (projectOptional.isEmpty()) {
      throw new NotFoundException("Project not found");
    }

    Project project = projectOptional.get();
    project.setName(newName);
    projectRepository.save(project);

    return projectMapper.toDto(project);
  }

  @Override
  public ProjectDto changeDescription(UUID projectId, String newDescription) {
    Optional<Project> projectOptional = projectRepository.findById(projectId);
    if (projectOptional.isEmpty()) {
      throw new NotFoundException("Project not found");
    }

    Project project = projectOptional.get();
    project.setDescription(newDescription);
    projectRepository.save(project);

    return projectMapper.toDto(project);
  }
}
