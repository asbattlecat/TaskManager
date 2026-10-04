package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.response.AuditEntityDto;
import org.example.taskmanager.taskmanager.controller.dto.response.CommentDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TagDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TaskDto;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEvent;
import org.example.taskmanager.taskmanager.domain.entity.*;
import org.example.taskmanager.taskmanager.domain.enums.*;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEventPublisher;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.specification.TaskSpecification;
import org.example.taskmanager.taskmanager.mapper.AuditEntityMapper;
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
  private final AuditEntityRepository auditEntityRepository;

  private final WorkspaceAccessChecker workspaceAccessChecker;

  private final TaskMapper taskMapper;
  private final TagMapper tagMapper;
  private final CommentMapper commentMapper;
  private final AuditEntityMapper auditEntityMapper;

  private final DomainEventPublisher domainEventPublisher;

  public  TaskServiceImpl(TaskRepository taskRepository,
                          BoardColumnRepository boardColumnRepository, CommentRepository commentRepository,
                          AuditEntityRepository auditEntityRepository,
                          TagRepository tagRepository, WorkspaceAccessChecker workspaceAccessChecker,
                          TaskMapper taskMapper, TagMapper tagMapper, CommentMapper commentMapper,
                          AuditEntityMapper auditEntityMapper, DomainEventPublisher domainEventPublisher) {
    this.taskRepository = taskRepository;
    this.boardColumnRepository = boardColumnRepository;
    this.commentRepository = commentRepository;
    this.tagRepository = tagRepository;
    this.auditEntityRepository = auditEntityRepository;
    this.workspaceAccessChecker = workspaceAccessChecker;
    this.taskMapper = taskMapper;
    this.tagMapper = tagMapper;
    this.commentMapper = commentMapper;
    this.auditEntityMapper = auditEntityMapper;
    this.domainEventPublisher = domainEventPublisher;
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
  public TaskDto archive(UUID taskId, ArchiveReason reason) {
    Task task = getTask(taskId);

    if (task.getArchiveState() != ArchiveState.ACTIVE)
      throw new IllegalArgumentException("Task is already archived");

    task.archive(reason);
    taskRepository.save(task);

    return  taskMapper.toDto(task);
  }

  @Override
  public TaskDto unarchive(UUID taskId, UnarchiveReason reason) {
    Task task = getTask(taskId);

    if (task.getArchiveState() == ArchiveState.ACTIVE)
      throw new IllegalArgumentException("Task is already active");

    task.unarchive(reason);
    taskRepository.save(task);
    task.getEvents().forEach(domainEventPublisher::publish);
    task.clearEvents();

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto changeName(UUID taskId, String newName, UUID userId) {
    Task task = getTask(taskId);

    if (!task.getName().equals(newName)) {
      createAuditEntity(taskId, userId, "name", task.getName(), newName);

      task.setName(newName);
      taskRepository.save(task);
    }

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto changeDescription(UUID taskId, String newDescription, UUID userId) {
    Task task = getTask(taskId);

    if (!task.getDescription().equals(newDescription)) {
      createAuditEntity(taskId, userId, "description", task.getDescription(), newDescription);

      task.setDescription(newDescription);
      taskRepository.save(task);
    }

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto changePriority(UUID taskId, TaskPriority newPriority, UUID userId) {
    Task task = getTask(taskId);

    if (!task.getPriority().equals(newPriority)) {
      createAuditEntity(taskId, userId, "priority",
              task.getPriority().toString(), newPriority.toString());

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
  public TaskDto changeDeadline(UUID taskId, Instant newDeadline, UUID userId) {
    Task task = getTask(taskId);

    if (!task.getDeadline().isAfter(newDeadline) && !task.getDeadline().equals(newDeadline)) {
      createAuditEntity(taskId, userId, "deadline",
              task.getDeadline().toString(), newDeadline.toString());

      task.setDeadline(newDeadline);
      taskRepository.save(task);
    }

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto changeStatus(UUID taskId, TaskStatus newStatus, UUID userId) {
    Task task = getTask(taskId);

    if (!task.getStatus().equals(newStatus)) {
      createAuditEntity(taskId, userId, "status",
              task.getStatus().toString(), newStatus.toString());

      task.setStatus(newStatus);
      taskRepository.save(task);
    }

    return taskMapper.toDto(task);
  }

  @Override
  public TaskDto setAssignee(UUID taskId, UUID workspaceMemberId, UUID userId) {
    Task task = getTask(taskId);

    workspaceAccessChecker.check(task, workspaceMemberId);

    if (!task.getAssigneeId().equals(workspaceMemberId)) {
      createAuditEntity(taskId, userId, "assignee",
              task.getAssigneeId().toString(), workspaceMemberId.toString());

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

  @Override
  public List<AuditEntityDto> getTaskChangesHistory(UUID taskId) {
    List<AuditEntity> entities = auditEntityRepository.findAllByTaskIdOrderByTimestampAsc(taskId);

    if (entities.isEmpty()) {
      throw new NotFoundException("Task changes history is empty");
    }

    return entities.stream().map(auditEntityMapper::toDto).toList();
  }

  private Task getTask(UUID taskId) {
    return taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException("Task not found"));
  }

  private void createAuditEntity(UUID taskId, UUID userId, String fieldName, String oldValue, String newValue) {
    AuditEntity entity = new AuditEntity(taskId, userId, fieldName, oldValue, newValue);
    auditEntityRepository.save(entity);
  }
}
