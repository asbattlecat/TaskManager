package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.AuditEntityDto;
import org.example.taskmanager.taskmanager.controller.dto.response.CommentDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TagDto;
import org.example.taskmanager.taskmanager.controller.dto.response.TaskDto;
import org.example.taskmanager.taskmanager.domain.entity.*;
import org.example.taskmanager.taskmanager.domain.enums.*;
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
  private final BoardColumnRepository boardColumnRepository;
  private final DomainEventPublisher domainEventPublisher;
  private final AuditEntityService auditEntityService;
  private final CommentRepository commentRepository;
  private final BoardRepository boardRepository;
  private final TaskRepository taskRepository;
  private final TagRepository tagRepository;
  private final CommentMapper commentMapper;
  private final TaskMapper taskMapper;
  private final TagMapper tagMapper;

  public TaskServiceImpl(
          WorkspaceAccessChecker workspaceAccessChecker,
          BoardColumnRepository boardColumnRepository,
          DomainEventPublisher domainEventPublisher,
          AuditEntityService auditEntityService,
          CommentRepository commentRepository,
          BoardRepository boardRepository,
          TaskRepository taskRepository,
          TagRepository tagRepository,
          CommentMapper commentMapper,
          TaskMapper taskMapper,
          TagMapper tagMapper
  ) {
    this.workspaceAccessChecker = workspaceAccessChecker;
    this.boardColumnRepository = boardColumnRepository;
    this.domainEventPublisher = domainEventPublisher;
    this.auditEntityService = auditEntityService;
    this.commentRepository = commentRepository;
    this.boardRepository = boardRepository;
    this.taskRepository = taskRepository;
    this.tagRepository = tagRepository;
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

    if (!boardRepository.existsById(boardId)) {
      log.error("task was not created, board not found, boardId={}", boardId);
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

    log.debug("task archive operation completed, taskId={}, reason={}", taskId, reason);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public List<TaskDto> archiveByBoard(UUID boardId, ArchiveReason reason) {
    log.info("task archiveByBoard starts, boardId={}, reason={}", boardId, reason);

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

    log.debug("task archiveByBoard complete, boardId={}, reason={}", boardId, reason);
    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public TaskDto unarchive(UUID taskId, UnarchiveReason reason) {
    log.info("board unarchive starts, taskId={}, reason={}", taskId, reason);

    Task task = getTask(taskId, "unarchive");

    ArchiveState oldValue = task.getArchiveState();

    Board parent = getParent(task.getBoardId(), "unarchive");
    checkParentBeforeUnarchive(parent);

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

    log.debug("task unarchive completed, taskId={}, reason={}", taskId, reason);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public List<TaskDto> unarchiveByBoard(UUID boardId, UnarchiveReason reason) {
    log.info("task unarchiveByBoard starts, boardId={}, reason={}", boardId, reason);

    List<Task> tasks = taskRepository
            .findTasksByBoardIdWithPessimisticWriteLock(boardId);

    Board parent = getParent(boardId, "unarchiveByBoard");
    checkParentBeforeUnarchive(parent);

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

    log.debug("task unarchiveByBoard completed, boardId={}, reason={}", boardId, reason);
    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public TaskDto changeName(UUID taskId, String newName, UUID userId) {
    log.info("task changeName operation starts, taskId={}, newName={}, userId={}", taskId, newName, userId);
    Task task = getTask(taskId, "changeName");

    isTaskActive(task, "changeName");

    if (!task.getName().equals(newName)) {
      auditEntityService.create(
              taskId,
              userId,
              "name",
              task.getName(),
              newName
      );

      task.setName(newName);
      taskRepository.save(task);
    }

    log.debug("task changeName operation completed, taskId={}, newName={}, userId={}", taskId, newName, userId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeDescription(UUID taskId, String newDescription, UUID userId) {
    log.info("task changeDescription operation starts, taskId={}, newDescription={}, userId={}",
            taskId, newDescription, userId);
    Task task = getTask(taskId, "changeDescription");

    isTaskActive(task, "changeDescription");

    if (!task.getDescription().equals(newDescription)) {
      auditEntityService.create(
              taskId,
              userId,
              "description",
              task.getDescription(),
              newDescription
      );

      task.setDescription(newDescription);
      taskRepository.save(task);
    }

    log.debug("task changeDescription operation completed, taskId={}, newDescription={}, userId={}",
            taskId, newDescription, userId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changePriority(UUID taskId, TaskPriority newPriority, UUID userId) {
    log.info("task changePriority operation starts, taskId={}, newPriority={}, userId={}",
            taskId, newPriority, userId);
    Task task = getTask(taskId, "changePriority");

    isTaskActive(task, "changePriority");

    if (!task.getPriority().equals(newPriority)) {
      auditEntityService.create(
              taskId,
              userId,
              "priority",
              task.getPriority().toString(),
              newPriority.toString()
      );

      task.setPriority(newPriority);
      taskRepository.save(task);
    }

    log.debug("task changePriority operation completed, taskId={}, newPriority={}, userId={}",
            taskId, newPriority, userId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeColumn(UUID taskId, UUID newColumnId) {
    log.info("task changeColumn operation starts, taskId={}, newColumnId={}", taskId, newColumnId);
    Task task = getTask(taskId, "changeColumn");

    isTaskActive(task, "changeColumn");

    if (boardColumnRepository.findById(newColumnId).isEmpty()) {
      throw new NotFoundException("Column with id " + newColumnId + " not found");
    }
    if (!task.getColumnId().equals(newColumnId)) {
      task.setColumnId(newColumnId);
      taskRepository.save(task);
    }

    log.debug("task changeColumn operation completed, taskId={}, newColumnId={}", taskId, newColumnId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeDeadline(UUID taskId, Instant newDeadline, UUID userId) {
    log.info("task changeDeadline operation starts, taskId={}, newDeadline={}, userId={}",
            taskId, newDeadline, userId);
    Task task = getTask(taskId, "changeDeadline");

    isTaskActive(task, "changeDeadline");

    if (!task.getDeadline().isAfter(newDeadline) && !task.getDeadline().equals(newDeadline)) {
      auditEntityService.create(
              taskId,
              userId,
              "deadline",
              task.getDeadline().toString(),
              newDeadline.toString()
      );

      task.setDeadline(newDeadline);
      taskRepository.save(task);
    }

    log.debug("task changeDeadline operation completed, taskId={}, newDeadline={}, userId={}",
            taskId, newDeadline, userId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeStatus(UUID taskId, TaskStatus newStatus, UUID userId) {
    log.info("task changeStatus operation starts, taskId={}, newStatus={}, userId={}",
            taskId, newStatus, userId);
    Task task = getTask(taskId, "changeStatus");

    isTaskActive(task, "changeStatus");

    if (!task.getStatus().equals(newStatus)) {
      auditEntityService.create(
              taskId,
              userId,
              "status",
              task.getStatus().toString(),
              newStatus.toString()
      );

      task.setStatus(newStatus);
      taskRepository.save(task);
    }

    log.debug("task changeStatus operation completed, taskId={}, newStatus={}, userId={}",
            taskId, newStatus, userId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto setAssignee(UUID taskId, UUID workspaceMemberId, UUID userId) {
    log.info("task setAssignee operation starts, taskId={}, workspaceMemberId={}, userId={}",
            taskId, workspaceMemberId, userId);
    Task task = getTask(taskId, "setAssignee");

    isTaskActive(task, "setAssignee");

    workspaceAccessChecker.check(task, workspaceMemberId);

    if (!task.getAssigneeId().equals(workspaceMemberId)) {
      auditEntityService.create(
              taskId,
              userId,
              "assignee",
              task.getAssigneeId().toString(),
              workspaceMemberId.toString()
      );

      task.setAssigneeId(workspaceMemberId);
      taskRepository.save(task);
    }

    log.debug("task setAssignee operation completed, taskId={}, workspaceMemberId={}, userId={}",
            taskId, workspaceMemberId, userId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public CommentDto addComment(UUID taskId, UUID workspaceMemberId, String content) {
    log.info("task addComment operation starts, taskId={}, workspaceMemberId={}, content={}",
            taskId, workspaceMemberId, content);
    Task task = getTask(taskId, "addComment");

    workspaceAccessChecker.check(task, workspaceMemberId);

//    Comment comment = new Comment(taskId, workspaceMemberId, content);
//    commentRepository.save(comment);

    log.debug("task addComment operation completed, taskId={}, workspaceMemberId={}, content={}",
            taskId, workspaceMemberId, content);
    return commentMapper.toDto(comment);
  }

  @Transactional
  @Override
  public TagDto addTag(UUID taskId, String name, TagColor color) {
    log.info("task addTag operation starts, taskId={}, name={}, color={}",
            taskId, name, color);
    getTask(taskId, "addTag");


    log.debug("task addTag operation completed, taskId={}, name={}, color={}",
            taskId, name, color);
//    return tagMapper.toDto(tag);
  }

  @Transactional
  @Override
  public List<TaskDto> filter(UUID boardId, TaskStatus status, UUID assigneeId, Tag tag) {
    log.info("task filter starts, boardId={}, status={}, assigneeId={}, tag={}",
            boardId, status, assigneeId, tag);

    Specification<Task> spec = Specification.where((Specification<Task>) null);

    if (boardId != null) spec = spec.and(TaskSpecification.boardContains(boardId));
    if (status != null) spec = spec.and(TaskSpecification.statusContains(status));
    if (assigneeId != null) spec = spec.and(TaskSpecification.assigneeContains(assigneeId));
    if (tag != null) spec = spec.and(TaskSpecification.tagContains(tag));

    List<Task> tasks = taskRepository.findAll(spec);

    log.info("task filter completed, boardId={}, status={}, assigneeId={}, tag={}",
            boardId, status, assigneeId, tag);
    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public List<AuditEntityDto> getTaskChangesHistory(UUID taskId) {
    log.info("task getTaskChangesHistory operation starts, taskId={}", taskId);
    return auditEntityService.getTaskChangesHistory(taskId);
  }

  private Task getTask(UUID taskId, String operationName) {
    return taskRepository
            .findById(taskId)
            .orElseThrow(() -> {
              log.error("task not found during {}, taskId={}", operationName, taskId);
              return new NotFoundException("Task with id " + taskId
                      + " not found during " + operationName + "operation");
            });
  }

  private Board getParent(UUID boardId, String operationName) {
    return boardRepository
            .findById(boardId)
            .orElseThrow(() -> {
              log.error("board not found during {}, boardId={}", operationName, boardId);
              return new NotFoundException("Board with id " + boardId
                      + " as parent of task not found during " + operationName + " operation");
            });
  }

  private void checkParentBeforeUnarchive(Board parent) {
    if (parent.getArchiveState() != ArchiveState.ACTIVE) {
      log.error("cannot unarchive tasks, board (parent) is archived, boardId={}", parent.getId());
      throw new IllegalStateException("Cannot unarchive tasks because board (parent) with id "
              + parent.getId() + " is archived");
    }
  }

  private void isTaskActive(Task task, String operationName) {
    if (task.getArchiveState() != ArchiveState.ACTIVE) {
      log.error("cannot do {}, task is archived, taskId={}", operationName, task.getId());
      throw new IllegalStateException("Cannot do " + operationName
              + " operation because task with id " + task.getId() + " is archived");
    }
  }
}
