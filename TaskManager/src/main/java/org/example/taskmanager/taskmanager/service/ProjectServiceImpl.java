package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.response.BoardDto;
import org.example.taskmanager.taskmanager.controller.dto.response.ProjectDto;
import org.example.taskmanager.taskmanager.domain.entity.Workspace;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEvent;
import org.example.taskmanager.taskmanager.domain.entity.Board;
import org.example.taskmanager.taskmanager.domain.entity.Project;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.domain.enums.ArchiveState;
import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEventPublisher;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.BoardMapper;
import org.example.taskmanager.taskmanager.mapper.ProjectMapper;
import org.example.taskmanager.taskmanager.repository.BoardRepository;
import org.example.taskmanager.taskmanager.repository.ProjectRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.ProjectService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectServiceImpl implements ProjectService {
  private final WorkspaceRepository  workspaceRepository;
  private final ProjectRepository projectRepository;
  private final BoardRepository boardRepository;
  private final ProjectMapper projectMapper;
  private final BoardMapper boardMapper;

  private final DomainEventPublisher domainEventPublisher;

  public ProjectServiceImpl(WorkspaceRepository workspaceRepository, ProjectRepository projectRepository,
                            BoardRepository boardRepository, ProjectMapper projectMapper,
                            BoardMapper boardMapper, DomainEventPublisher domainEventPublisher) {
    this.workspaceRepository = workspaceRepository;
    this.projectRepository = projectRepository;
    this.boardRepository = boardRepository;
    this.projectMapper = projectMapper;
    this.boardMapper = boardMapper;

    this.domainEventPublisher = domainEventPublisher;
  }


  @Override
  public ProjectDto create(UUID workspaceId, String name, String description) {
    if (!workspaceRepository.existsById(workspaceId))
      throw new NotFoundException("Workspace not found");

    Project project = new Project(workspaceId, name, description);
    projectRepository.save(project);

    return projectMapper.toDto(project);
  }

  @Override
  public ProjectDto archive(UUID projectId, ArchiveReason reason) {
    Project project = getProject(projectId);

    if (project.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalArgumentException("Project is already archived");

    project.archive(reason);
    projectRepository.save(project);
    project.getEvents().forEach(domainEventPublisher::publish);;
    project.clearEvents();

    return projectMapper.toDto(project);
  }

  @Override
  public List<ProjectDto> archiveByWorkspace(UUID workspaceId, ArchiveReason reason) {
    List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);

    if (projects.isEmpty()) throw new NotFoundException("Projects not found");

    for (Project project : projects) {
      if (project.getArchiveState() == ArchiveState.ACTIVE) {
        project.archive(reason);
        projectRepository.save(project);
        project.getEvents().forEach(domainEventPublisher::publish);;
        project.clearEvents();
      }
    }

    return projects.stream().map(projectMapper::toDto).toList();
  }

  @Override
  public ProjectDto unarchive(UUID projectId, UnarchiveReason reason) {
    Project project = getProject(projectId);
    Workspace parent = workspaceRepository.findById(project.getWorkspaceId())
            .orElseThrow(() -> new NotFoundException("Workspace not found"));

    if (parent.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalStateException("Cannot unarchive project because workspace is archived");

    project.unarchive(reason);
    projectRepository.save(project);
    project.getEvents().forEach(domainEventPublisher::publish);
    project.clearEvents();

    return projectMapper.toDto(project);
  }

  @Override
  public List<ProjectDto> unarchiveByWorkspace(UUID workspaceId, UnarchiveReason reason) {
    Workspace parent = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found"));
    List<Project> projects = projectRepository.findAllByWorkspaceId(workspaceId);
    if (projects.isEmpty()) throw new NotFoundException("Projects not found");

    if (parent.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalStateException("Cannot unarchive projects because workspace is archived");

    for (Project project : projects) {
      if (project.canBeUnarchived(reason)) {
        project.unarchive(reason);
        projectRepository.save(project);
        project.getEvents().forEach(domainEventPublisher::publish);
        project.clearEvents();
      }
    }

    return projects.stream().map(projectMapper::toDto).toList();
  }

  @Override
  public List<BoardDto> getProjectBoards(UUID projectId) {
    List<Board> boards = boardRepository.findBoardsByProjectId(projectId);
    if (boards.isEmpty()) {
      throw new NotFoundException("Boards not found");
    }

    return boards.stream().map(boardMapper::toDto).toList();
  }

  @Override
  public ProjectDto changeName(UUID projectId, String newName) {
    Project project = getProject(projectId);

    project.setName(newName);
    projectRepository.save(project);

    return projectMapper.toDto(project);
  }

  @Override
  public ProjectDto changeDescription(UUID projectId, String newDescription) {
    Project project = getProject(projectId);

    project.setDescription(newDescription);
    projectRepository.save(project);

    return projectMapper.toDto(project);
  }

  private Project getProject(UUID projectId) {
    return projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));
  }
}
