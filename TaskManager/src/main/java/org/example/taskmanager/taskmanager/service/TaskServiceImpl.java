package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.AuditEntityDto;
import org.example.taskmanager.taskmanager.controller.dto.response.CommentDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TagDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TaskDto;
import org.example.taskmanager.taskmanager.domain.entity.*;
import org.example.taskmanager.taskmanager.domain.enums.*;
import org.example.taskmanager.taskmanager.domain.port.BoardAccessPort;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEventPublisher;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.specification.TaskSpecification;
import org.example.taskmanager.taskmanager.mapper.CommentMapper;
import org.example.taskmanager.taskmanager.mapper.TagMapper;
import org.example.taskmanager.taskmanager.service.interfaces.AuditEntityService;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceAccessChecker;
import org.example.taskmanager.taskmanager.mapper.TaskMapper;
import org.example.taskmanager.taskmanager.repository.*;
import org.example.taskmanager.taskmanager.service.interfaces.TaskService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TaskServiceImpl implements TaskService {
  private final WorkspaceAccessChecker workspaceAccessChecker;
  private final DomainEventPublisher domainEventPublisher;
  private final AuditEntityService auditEntityService;
  private final BoardAccessPort boardAccessPort;
  private final TaskRepository taskRepository;
  private final CommentMapper commentMapper;
  private final TaskMapper taskMapper;
  private final TagMapper tagMapper;

  public TaskServiceImpl(
          WorkspaceAccessChecker workspaceAccessChecker,
          DomainEventPublisher domainEventPublisher,
          AuditEntityService auditEntityService,
          BoardAccessPort boardAccessPort,
          TaskRepository taskRepository,
          CommentMapper commentMapper,
          TaskMapper taskMapper,
          TagMapper tagMapper
  ) {
    this.workspaceAccessChecker = workspaceAccessChecker;
    this.domainEventPublisher = domainEventPublisher;
    this.auditEntityService = auditEntityService;
    this.boardAccessPort = boardAccessPort;
    this.taskRepository = taskRepository;
    this.commentMapper = commentMapper;
    this.taskMapper = taskMapper;
    this.tagMapper = tagMapper;
  }

  @Transactional
  @Override
  public TaskDto create(UUID boardId, UUID columnId, String name, String description,
                        TaskStatus status, TaskPriority priority, UUID assigneeId,
                        UUID creatorId, Instant deadline) {
    log.info("creating task, boardId={}, columnId={}, name={}, description={}, status={}," +
                    " priority={}, assigneeId={}, creatorId={}, deadline={}",
            boardId, columnId, name, description, status,
            priority, assigneeId, creatorId, deadline);

    if (!boardAccessPort.exists(boardId)) {
      throw new NotFoundException("Board with id " + boardId + "not found during task create operation");
    }

    Task task = new Task(boardId, columnId, name, description, status, priority,
            assigneeId, creatorId, deadline);

    taskRepository.save(task);

    log.debug("task created and saved in database, taskId={}", task.getId());
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto archive(UUID taskId, ArchiveReason reason) {
    log.info("task archive operation starts, taskId={}, reason={}", taskId, reason);

    Task task = getTask(taskId, "archive");
    ArchiveState oldValue = task.getArchiveState();

    task.archive(reason);
    taskRepository.save(task);

    auditEntityService.create(
            taskId,
            null,
            "archiveState",
            oldValue.toString(),
            task.getArchiveState().toString()
    );

    task.getEvents().forEach(domainEventPublisher::publish);
    task.clearEvents();

    log.debug("TaskService archive operation completed, taskId={}, reason={}", taskId, reason);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public List<TaskDto> archiveByBoard(UUID boardId, ArchiveReason reason) {
    log.info("task archiveByBoard starts, boardId={}, reason={}", boardId, reason);

    if (!boardAccessPort.exists(boardId)) {
      throw new IllegalArgumentException("Board with boardId=" + boardId + " not found during archiveByBoard operation");
    }
    List<Task> tasks = taskRepository
            .findTasksByBoardIdWithPessimisticWriteLock(boardId);

    for (Task task : tasks) {
      if (task.getArchiveState() != ArchiveState.ACTIVE) continue;

      ArchiveState oldValue = task.getArchiveState();
      task.archive(reason);
      taskRepository.save(task);

      auditEntityService.create(
              task.getId(),
              null,
              "archiveState",
              oldValue.toString(),
              task.getArchiveState().toString()
      );

      task.getEvents().forEach(domainEventPublisher::publish);
      task.clearEvents();

      log.debug("task with id={} archived by project and saved in database", task.getId());
    }

    log.debug("TaskService archiveByBoard completed, boardId={}, reason={}", boardId, reason);
    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public TaskDto unarchive(UUID taskId, UnarchiveReason reason) {
    log.info("board unarchive starts, taskId={}, reason={}", taskId, reason);

    Task task = getTask(taskId, "unarchive");
    ArchiveState oldValue = task.getArchiveState();
    boardAccessPort.requireActive(task.getBoardId(), "unarchive");

    task.unarchive(reason);
    taskRepository.save(task);

    auditEntityService.create(
            taskId,
            null,
            "archiveState",
            oldValue.toString(),
            task.getArchiveState().toString()
    );

    task.getEvents().forEach(domainEventPublisher::publish);
    task.clearEvents();

    log.debug("TaskService unarchive completed, taskId={}, reason={}", taskId, reason);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public List<TaskDto> unarchiveByBoard(UUID boardId, UnarchiveReason reason) {
    log.info("task unarchiveByBoard starts, boardId={}, reason={}", boardId, reason);

    List<Task> tasks = taskRepository
            .findTasksByBoardIdWithPessimisticWriteLock(boardId);
    boardAccessPort.requireActive(boardId, "unarchiveByBoard");

    for (Task task : tasks) {
      if (!task.canBeUnarchived(reason)) continue;
      ArchiveState oldValue = task.getArchiveState();

      task.unarchive(reason);
      taskRepository.save(task);

      auditEntityService.create(
              task.getId(),
              null,
              "archiveState",
              oldValue.toString(),
              task.getArchiveState().toString()
      );

      task.getEvents().forEach(domainEventPublisher::publish);
      task.clearEvents();

      log.debug("task with id={} unarchived by board and saved in database", task.getId());
    }

    log.debug("TaskService unarchiveByBoard completed, boardId={}, reason={}", boardId, reason);
    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public TaskDto changeName(UUID taskId, String newName, UUID userId) {
    Task task = getTask(taskId, "changeName");
    isTaskActive(task, "changeName");

    if (!task.getName().equals(newName)) {
      String old = task.getName();
      task.changeName(newName);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              userId,
              "name",
              old,
              newName
      );
    }

    log.debug("TaskService changeName completed, taskId={}, newName={}, userId={}", taskId, newName, userId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeDescription(UUID taskId, String newDescription, UUID userId) {
    Task task = getTask(taskId, "changeDescription");
    isTaskActive(task, "changeDescription");

    if (!task.getDescription().equals(newDescription)) {
      String old = task.getDescription();
      task.changeDescription(newDescription);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              userId,
              "description",
              old,
              newDescription
      );
    }

    log.debug("TaskService changeDescription completed, taskId={}, newDescription={}, userId={}",
            taskId, newDescription, userId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changePriority(UUID taskId, TaskPriority newPriority, UUID userId) {
    Task task = getTask(taskId, "changePriority");
    isTaskActive(task, "changePriority");

    if (!task.getPriority().equals(newPriority)) {
      TaskPriority old = task.getPriority();
      task.changePriority(newPriority);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              userId,
              "priority",
              old.toString(),
              newPriority.toString()
      );
    }

    log.debug("TaskService changePriority completed, taskId={}, newPriority={}, userId={}",
            taskId, newPriority, userId);
    return taskMapper.toDto(task);
  }

  /**
   * @param taskId id of task
   * @param newColumnId id of column. Note: Must exist, check before call this method
   * @return TaskDto
   */
  @Transactional
  @Override
  public TaskDto changeColumnId(UUID taskId, UUID newColumnId, UUID userId) {
    Task task = getTask(taskId, "changeColumn");
    isTaskActive(task, "changeColumn");

    if (!task.getColumnId().equals(newColumnId)) {
      UUID old = task.getColumnId();
      task.changeColumnId(newColumnId);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              userId,
              "columnId",
              old.toString(),
              newColumnId.toString()
      );
    }

    log.debug("TaskService changeColumn completed, taskId={}, newColumnId={}", taskId, newColumnId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeDeadline(UUID taskId, Instant newDeadline, UUID userId) {
    Task task = getTask(taskId, "changeDeadline");

    isTaskActive(task, "changeDeadline");

    if (!task.getDeadline().isAfter(newDeadline) && !task.getDeadline().equals(newDeadline)) {
      Instant old = task.getDeadline();
      task.changeDeadline(newDeadline);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              userId,
              "deadline",
              old.toString(),
              newDeadline.toString()
      );
    }

    log.debug("TaskService changeDeadline completed, taskId={}, newDeadline={}, userId={}",
            taskId, newDeadline, userId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeStatus(UUID taskId, TaskStatus newStatus, UUID userId) {
    log.info("TaskService changeStatus starts, taskId={}, newStatus={}, userId={}",
            taskId, newStatus, userId);
    Task task = getTask(taskId, "changeStatus");
    isTaskActive(task, "changeStatus");

    if (!task.getStatus().equals(newStatus)) {
      TaskStatus old = task.getStatus();
      task.changeStatus(newStatus);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              userId,
              "status",
              old.toString(),
              newStatus.toString()
      );
    }

    log.debug("TaskService changeStatus completed, taskId={}, newStatus={}, userId={}",
            taskId, newStatus, userId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto setAssignee(UUID taskId, UUID workspaceMemberId, UUID userId) {
    log.info("TaskService setAssignee starts, taskId={}, workspaceMemberId={}, userId={}",
            taskId, workspaceMemberId, userId);
    Task task = getTask(taskId, "setAssignee");
    isTaskActive(task, "setAssignee");

    workspaceAccessChecker.check(task, workspaceMemberId);

    if (!task.getAssigneeId().equals(workspaceMemberId)) {
      UUID old = task.getAssigneeId();
      task.changeAssigneeId(workspaceMemberId);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              userId,
              "assignee",
              old.toString(),
              workspaceMemberId.toString()
      );
    }

    log.debug("TaskService setAssignee completed, taskId={}, workspaceMemberId={}, userId={}",
            taskId, workspaceMemberId, userId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public CommentDto addComment(UUID taskId, UUID workspaceMemberId, String content) {
    Task task = getTask(taskId, "addComment");
    isTaskActive(task, "addComment");
    workspaceAccessChecker.check(task, workspaceMemberId);

    Comment comment = task.addComment(workspaceMemberId, content);
    taskRepository.save(task);

    log.debug("TaskService addComment completed, taskId={}, workspaceMemberId={}, content={}",
            taskId, workspaceMemberId, content);
    return commentMapper.toDto(comment);
  }

  @Transactional
  @Override
  public TagDto addTag(UUID taskId, String name, TagColor color) {
    Task task = getTask(taskId, "addTag");
    isTaskActive(task, "addTag");

    Tag tag = task.addTag(name, color);
    taskRepository.save(task);

    log.debug("TaskService addTag completed, taskId={}, name={}, color={}", taskId, name, color);
    return tagMapper.toDto(tag);
  }

  @Transactional
  @Override
  public List<TaskDto> filter(UUID boardId, TaskStatus status, UUID assigneeId, Tag tag) {
    Specification<Task> spec = Specification.where((Specification<Task>) null);

    if (boardId != null) spec = spec.and(TaskSpecification.boardContains(boardId));
    if (status != null) spec = spec.and(TaskSpecification.statusContains(status));
    if (assigneeId != null) spec = spec.and(TaskSpecification.assigneeContains(assigneeId));
    if (tag != null) spec = spec.and(TaskSpecification.tagContains(tag));

    List<Task> tasks = taskRepository.findAll(spec);

    log.info("TaskService filter completed, boardId={}, status={}, assigneeId={}, tag={}",
            boardId, status, assigneeId, tag);
    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public List<AuditEntityDto> getTaskChangesHistory(UUID taskId) {
    return auditEntityService.getTaskChangesHistory(taskId);
  }

  // ====================== вспомогательные методы ===================

  private Task getTask(UUID taskId, String operationName) {
    return taskRepository
            .findById(taskId)
            .orElseThrow(() -> new NotFoundException("Task with id " + taskId
                    + " not found during " + operationName + "operation"));
  }


  private void isTaskActive(Task task, String operationName) {
    if (task.getArchiveState() != ArchiveState.ACTIVE) {
      throw new IllegalStateException("Cannot do " + operationName
              + " operation because task with id " + task.getId() + " is archived");
    }
  }
}
