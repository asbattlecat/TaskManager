package org.example.taskmanager.taskmanager.service;

import jakarta.validation.constraints.NotNull;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
  public TaskDto create(@NotNull UUID boardId, UUID columnId, @NotNull String name, String description,
                        @NotNull TaskStatus status, @NotNull TaskPriority priority, UUID assigneeMemberId,
                        @NotNull UUID creatorMemberId, Instant deadline) {
    log.info("creating task, boardId={}, columnId={}, name={}, description={}, status={}," +
                    " priority={}, assigneeMemberId={}, creatorMemberId={}, deadline={}",
            boardId, columnId, name, description, status,
            priority, assigneeMemberId, creatorMemberId, deadline);
    // board должен быть активным
    boardAccessPort.requireActive(boardId, "task create");

    // значит это kanban доска
    if (columnId != null) {
      if (!boardAccessPort.isBoardKanban(boardId)) {
        throw new IllegalArgumentException("Board type with id=" + boardId
                + " is not kanban, cannot create task as part of column with id=" + columnId);
      }
      // проверяем, что указанная колонка является частью board
      boardAccessPort.requireColumnOfBoard(boardId, columnId);
    }

    if (assigneeMemberId != null) {
      // проверяем, что у создателя и выполняющего есть доступ к этому workspace (он member)
      workspaceAccessChecker.checkTwo(boardId, creatorMemberId, assigneeMemberId);
    } else {
      // тут уже просто чек на то, что creator - member этого workspace
      workspaceAccessChecker.check(boardId, creatorMemberId);
    }

    Task task = new Task(boardId, columnId, name, description, status, priority,
            assigneeMemberId, creatorMemberId, deadline);
    taskRepository.save(task);

    log.debug("task created and saved in database, taskId={}", task.getId());
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto archive(UUID taskId, ArchiveReason reason) {
    log.info("task archive operation starts, taskId={}, reason={}", taskId, reason);
    Task task = getTask(taskId, "archive");
    if (reason != ArchiveReason.USER_ACTION) {
      throw new IllegalArgumentException("Cannot archive task by "
              + reason + " reason, id=" + task.getId());
    }

    ArchiveState oldValue = task.getArchiveState();

    task.archive(reason);
    taskRepository.save(task);

    auditEntityService.create(
            taskId,
            null,
            "archiveState",
            String.valueOf(oldValue),
            String.valueOf(task.getArchiveState())
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
    List<Task> result = new ArrayList<>();

    for (Task task : tasks) {
      if (task.getArchiveState() != ArchiveState.ACTIVE) continue;

      ArchiveState oldValue = task.getArchiveState();
      task.archive(reason);
      taskRepository.save(task);

      auditEntityService.create(
              task.getId(),
              null,
              "archiveState",
              String.valueOf(oldValue),
              String.valueOf(task.getArchiveState())
      );

      task.getEvents().forEach(domainEventPublisher::publish);
      task.clearEvents();
      result.add(task);

      log.debug("task with id={} archived by project and saved in database", task.getId());
    }

    log.debug("TaskService archiveByBoard completed, boardId={}, reason={}", boardId, reason);
    return result.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public TaskDto unarchive(UUID taskId, UnarchiveReason reason) {
    log.info("board unarchive starts, taskId={}, reason={}", taskId, reason);
    Task task = getTask(taskId, "unarchive");
    if (reason != UnarchiveReason.USER_ACTION) {
      throw new IllegalArgumentException("Cannot unarchive task by "
              + reason + " reason, id=" + task.getId());
    }

    ArchiveState oldValue = task.getArchiveState();
    boardAccessPort.requireActive(task.getBoardId(), "unarchive");

    task.unarchive(reason);
    taskRepository.save(task);

    auditEntityService.create(
            taskId,
            null,
            "archiveState",
            String.valueOf(oldValue),
            String.valueOf(task.getArchiveState())
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
    List<Task> result = new ArrayList<>();

    for (Task task : tasks) {
      if (!task.canBeUnarchived(reason)) continue;
      ArchiveState oldValue = task.getArchiveState();

      task.unarchive(reason);
      taskRepository.save(task);

      auditEntityService.create(
              task.getId(),
              null,
              "archiveState",
              String.valueOf(oldValue),
              String.valueOf(task.getArchiveState())
      );

      task.getEvents().forEach(domainEventPublisher::publish);
      task.clearEvents();

      result.add(task);
      log.debug("task with id={} unarchived by board and saved in database", task.getId());
    }

    log.debug("TaskService unarchiveByBoard completed, boardId={}, reason={}", boardId, reason);
    return result.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public TaskDto changeName(UUID taskId, String newName, UUID memberId) {
    Task task = getTask(taskId, "changeName");
    isTaskActive(task, "changeName");
    workspaceAccessChecker.check(task.getBoardId(), memberId);

    if (!Objects.equals(task.getName(), newName)) {
      String old = task.getName();
      task.changeName(newName);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              memberId,
              "name",
              old,
              newName
      );
    }

    log.debug("TaskService changeName completed, taskId={}, newName={}, memberId={}", taskId, newName, memberId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeDescription(UUID taskId, String newDescription, UUID memberId) {
    Task task = getTask(taskId, "changeDescription");
    isTaskActive(task, "changeDescription");
    workspaceAccessChecker.check(task.getBoardId(), memberId);

    if (!Objects.equals(task.getDescription(),newDescription)) {
      String old = task.getDescription();
      task.changeDescription(newDescription);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              memberId,
              "description",
              old,
              newDescription
      );
    }

    log.debug("TaskService changeDescription completed, taskId={}, newDescription={}, memberId={}",
            taskId, newDescription, memberId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changePriority(UUID taskId, TaskPriority newPriority, UUID memberId) {
    Task task = getTask(taskId, "changePriority");
    isTaskActive(task, "changePriority");
    workspaceAccessChecker.check(task.getBoardId(), memberId);

    if (!Objects.equals(task.getPriority(),newPriority)) {
      TaskPriority old = task.getPriority();
      task.changePriority(newPriority);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              memberId,
              "priority",
              String.valueOf(old),
              String.valueOf(newPriority)
      );
    }

    log.debug("TaskService changePriority completed, taskId={}, newPriority={}, memberId={}",
            taskId, newPriority, memberId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeColumnId(UUID taskId, UUID newColumnId, UUID memberId) {
    Task task = getTask(taskId, "changeColumn");
    // активен ли Task
    isTaskActive(task, "changeColumn");
    // WorkspaceMember с указанным memberId является участником Workspace по Task
    workspaceAccessChecker.check(task.getBoardId(), memberId);
    boardAccessPort.isBoardKanban(task.getBoardId());

    if (!Objects.equals(task.getColumnId(),newColumnId)) {
      // новая колонка часть того же board
      boardAccessPort.requireColumnOfBoard(task.getBoardId(), newColumnId);

      UUID old = task.getColumnId();
      task.changeColumnId(newColumnId);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              memberId,
              "columnId",
              String.valueOf(old),
              String.valueOf(newColumnId)
      );
    }

    log.debug("TaskService changeColumn completed, taskId={}, newColumnId={}, memberId={}",
            taskId, newColumnId, memberId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeDeadline(UUID taskId, Instant newDeadline, UUID memberId) {
    Task task = getTask(taskId, "changeDeadline");

    isTaskActive(task, "changeDeadline");
    workspaceAccessChecker.check(task.getBoardId(), memberId);

    if (!Objects.equals(task.getDeadline(),newDeadline)) {
      Instant old = task.getDeadline();
      task.changeDeadline(newDeadline);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              memberId,
              "deadline",
              String.valueOf(old),
              String.valueOf(newDeadline)
      );
    }

    log.debug("TaskService changeDeadline completed, taskId={}, newDeadline={}, memberId={}",
            taskId, newDeadline, memberId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeStatus(UUID taskId, TaskStatus newStatus, UUID memberId) {
    log.info("TaskService changeStatus starts, taskId={}, newStatus={}, memberId={}",
            taskId, newStatus, memberId);
    Task task = getTask(taskId, "changeStatus");

    isTaskActive(task, "changeStatus");
    workspaceAccessChecker.check(task.getBoardId(), memberId);

    if (task.getStatus() != newStatus) {
      TaskStatus old = task.getStatus();
      task.changeStatus(newStatus);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              memberId,
              "status",
              String.valueOf(old),
              String.valueOf(newStatus)
      );
    }

    log.debug("TaskService changeStatus completed, taskId={}, newStatus={}, memberId={}",
            taskId, newStatus, memberId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public TaskDto changeAssigneeId(UUID taskId, UUID newAssigneeId, UUID memberId) {
    log.info("TaskService setAssignee starts, taskId={}, workspaceMemberId={}, memberId={}",
            taskId, newAssigneeId, memberId);
    Task task = getTask(taskId, "setAssignee");

    isTaskActive(task, "setAssignee");
    workspaceAccessChecker.checkTwo(task.getBoardId(), newAssigneeId, memberId);

    if (!Objects.equals(task.getAssigneeId(),newAssigneeId)) {
      UUID old = task.getAssigneeId();
      task.changeAssigneeId(newAssigneeId);
      taskRepository.save(task);

      auditEntityService.create(
              taskId,
              memberId,
              "assignee",
              String.valueOf(old),
              String.valueOf(newAssigneeId)
      );
    }

    log.debug("TaskService setAssignee completed, taskId={}, newAssigneeId={}, memberId={}",
            taskId, newAssigneeId, memberId);
    return taskMapper.toDto(task);
  }

  @Transactional
  @Override
  public CommentDto addComment(UUID taskId, UUID workspaceMemberId, String content) {
    Task task = getTask(taskId, "addComment");

    isTaskActive(task, "addComment");
    workspaceAccessChecker.check(task.getBoardId(), workspaceMemberId);

    Comment comment = task.addComment(workspaceMemberId, content);
    taskRepository.save(task);

    log.debug("TaskService addComment completed, taskId={}, workspaceMemberId={}, content={}",
            taskId, workspaceMemberId, content);
    return commentMapper.toDto(comment);
  }

  @Transactional
  @Override
  public TagDto addTag(UUID taskId, String name, TagColor color, UUID memberId) {
    Task task = getTask(taskId, "addTag");

    isTaskActive(task, "addTag");
    workspaceAccessChecker.check(task.getBoardId(), memberId);

    Tag tag = task.addTag(name, color);
    taskRepository.save(task);

    log.debug("TaskService addTag completed, taskId={}, name={}, color={}, memberId={}",
            taskId, name, color, memberId);
    return tagMapper.toDto(tag);
  }

  // TODO добавить пагинацию
  // TODO добавить пагинацию
  // TODO добавить пагинацию
  @Transactional(readOnly = true)
  @Override
  public List<TaskDto> filter(UUID boardId, TaskStatus status, UUID assigneeId, Tag tag) {
    Specification<Task> spec = Specification.unrestricted();

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
  public List<AuditEntityDto> getTaskChangesHistory(UUID taskId, UUID memberId) {
    Task task = getTask(taskId, "getTaskChangesHistory");
    workspaceAccessChecker.check(task.getBoardId(), memberId);
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
