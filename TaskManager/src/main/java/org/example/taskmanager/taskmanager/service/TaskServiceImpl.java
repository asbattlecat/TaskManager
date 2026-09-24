package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.CommentDto;
import org.example.taskmanager.taskmanager.controller.dto.TagDto;
import org.example.taskmanager.taskmanager.controller.dto.TaskDto;
import org.example.taskmanager.taskmanager.domain.entity.*;
import org.example.taskmanager.taskmanager.domain.enums.TagColor;
import org.example.taskmanager.taskmanager.domain.enums.TaskPriority;
import org.example.taskmanager.taskmanager.domain.enums.TaskStatus;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.specification.TaskSpecification;
import org.example.taskmanager.taskmanager.mapper.CommentMapper;
import org.example.taskmanager.taskmanager.mapper.TagMapper;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceAccessChecker;
import org.example.taskmanager.taskmanager.mapper.TaskMapper;
import org.example.taskmanager.taskmanager.repository.*;
import org.example.taskmanager.taskmanager.service.interfaces.TaskService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TaskServiceImpl implements TaskService {
  private final TaskRepository taskRepository;
  private final BoardColumnRepository boardColumnRepository;
  private final CommentRepository commentRepository;
  private final TagRepository tagRepository;

  private final WorkspaceAccessChecker workspaceAccessChecker;

  private final TaskMapper taskMapper;
  private final TagMapper tagMapper;
  private final CommentMapper commentMapper;

  public  TaskServiceImpl(TaskRepository taskRepository,
                          BoardColumnRepository boardColumnRepository, CommentRepository commentRepository,
                          TagRepository tagRepository, WorkspaceAccessChecker workspaceAccessChecker,
                          TaskMapper taskMapper, TagMapper tagMapper, CommentMapper commentMapper) {
    this.taskRepository = taskRepository;
    this.boardColumnRepository = boardColumnRepository;
    this.commentRepository = commentRepository;
    this.tagRepository = tagRepository;
    this.workspaceAccessChecker = workspaceAccessChecker;
    this.taskMapper = taskMapper;
    this.tagMapper = tagMapper;
    this.commentMapper = commentMapper;
  }


  @Override
  public TaskDto create(UUID boardId, UUID columnId, String name, String description,
                        TaskStatus status, TaskPriority priority, UUID assigneeId,
                        UUID creatorId, Instant deadline) {
    
    Task task = new Task(boardId, columnId, name, description, status, priority,
            assigneeId, creatorId, deadline);

    taskRepository.save(task);

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto changeName(UUID taskId, String newName) {
    Task task = getTask(taskId);

    if (!task.getName().equals(newName)) {
      task.setName(newName);
      taskRepository.save(task);
    }

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto changeDescription(UUID taskId, String newDescription) {
    Task task = getTask(taskId);

    if (!task.getDescription().equals(newDescription)) {
      task.setDescription(newDescription);
      taskRepository.save(task);
    }

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto changePriority(UUID taskId, TaskPriority newPriority) {
    Task task = getTask(taskId);

    if (!task.getPriority().equals(newPriority)) {
      task.setPriority(newPriority);
      taskRepository.save(task);
    }

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto changeColumn(UUID taskId, UUID newColumnId) {
    Task task = getTask(taskId);

    if (!boardColumnRepository.findById(newColumnId).isPresent()) {
      throw new NotFoundException("Column not found");
    }
    if (!task.getColumnId().equals(newColumnId)) {
      task.setColumnId(newColumnId);
      taskRepository.save(task);
    }

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto changeDeadline(UUID taskId, Instant newDeadline) {
    Task task = getTask(taskId);

    if (!task.getDeadline().isAfter(newDeadline) && !task.getDeadline().equals(newDeadline)) {
      task.setDeadline(newDeadline);
      taskRepository.save(task);
    }

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto changeStatus(UUID taskId, TaskStatus newStatus) {
    Task task = getTask(taskId);

    if (!task.getStatus().equals(newStatus)) {
      task.setStatus(newStatus);
      taskRepository.save(task);
    }

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto setAssignee(UUID taskId, UUID workspaceMemberId) {
    Task task = getTask(taskId);

    workspaceAccessChecker.check(task, workspaceMemberId);

    if (!task.getAssigneeId().equals(workspaceMemberId)) {
      task.setAssigneeId(workspaceMemberId);
      taskRepository.save(task);
    }

    return taskMapper.toDto(task);
  }

  @Override
  public CommentDto addComment(UUID taskId, UUID workspaceMemberId, String content) {
    Task task = getTask(taskId);

    workspaceAccessChecker.check(task, workspaceMemberId);

    Comment comment = new Comment(taskId, workspaceMemberId, content);
    commentRepository.save(comment);

    return commentMapper.toDto(comment);
  }

  @Override
  public TagDto addTag(UUID taskId, String name, TagColor color) {
    getTask(taskId);

    Tag tag = new Tag(taskId, name, color);
    tagRepository.save(tag);

    return tagMapper.toDto(tag);
  }

  @Override
  public List<TaskDto> filter(TaskStatus status, UUID assigneeId, Tag tag) {
    Specification<Task> spec = Specification.where((Specification<Task>) null);

    if (status != null) spec = spec.and(TaskSpecification.statusContains(status));
    if (assigneeId != null) spec = spec.and(TaskSpecification.assigneeContains(assigneeId));
    if (tag != null) spec = spec.and(TaskSpecification.tagContains(tag));

    List<Task> tasks = taskRepository.findAll(spec);

    return tasks.stream().map(taskMapper::toDto).toList();
  }

  private Task getTask(UUID taskId) {
    return taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException("Task not found"));
  }
}
