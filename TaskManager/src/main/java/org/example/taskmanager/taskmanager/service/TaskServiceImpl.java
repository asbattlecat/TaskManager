package org.example.taskmanager.taskmanager.service;

import jakarta.validation.constraints.NotBlank;
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
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@Validated
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
  public TaskDto create(@NotNull UUID boardId, UUID columnId, @NotBlank  String name, String description,
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
      // если создатель и выполняющий один и тот же - то можно оптимизировать за счет сокращения
      // запросов к бд
      if (Objects.equals(creatorMemberId, assigneeMemberId)) {
        workspaceAccessChecker.check(boardId, creatorMemberId);
      } else {
        workspaceAccessChecker.checkTwo(boardId, creatorMemberId, assigneeMemberId);
      }
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
  public TaskDto archive(@NotNull UUID taskId, @NotNull ArchiveReason reason, @NotNull UUID memberId) {
    log.info("task archive operation starts, taskId={}, reason={}", taskId, reason);
    Task task = getTask(taskId, "archive");
    if (reason != ArchiveReason.USER_ACTION) {
      throw new IllegalArgumentException("Cannot archive task by "
              + reason + " reason, id=" + task.getId());
    }
    workspaceAccessChecker.check(task.getBoardId(), memberId);
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
  public void archiveByBoard(@NotNull UUID boardId, @NotNull ArchiveReason reason) {
    log.info("task archiveByBoard starts, boardId={}, reason={}", boardId, reason);
    if (reason != ArchiveReason.PARENT_ARCHIVED) {
      throw new IllegalArgumentException("Cannot archiveByBoard, reason is incorrect, reason=" + reason);
    }
    if (!boardAccessPort.exists(boardId)) {
      throw new NotFoundException("Board with boardId=" + boardId + " not found during archiveByBoard operation");
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
              String.valueOf(oldValue),
              String.valueOf(task.getArchiveState())
      );

      task.getEvents().forEach(domainEventPublisher::publish);
      task.clearEvents();

      log.debug("task with id={} archived by project and saved in database", task.getId());
    }

    log.debug("TaskService archiveByBoard completed, boardId={}, reason={}", boardId, reason);
  }

  @Transactional
  @Override
  public TaskDto unarchive(@NotNull UUID taskId, @NotNull UnarchiveReason reason, @NotNull UUID memberId) {
    log.info("board unarchive starts, taskId={}, reason={}", taskId, reason);
    Task task = getTask(taskId, "unarchive");
    if (reason != UnarchiveReason.USER_ACTION) {
      throw new IllegalArgumentException("Cannot unarchive task by "
              + reason + " reason, id=" + task.getId());
    }
    workspaceAccessChecker.check(task.getBoardId(), memberId);

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
  public void unarchiveByBoard(@NotNull UUID boardId, @NotNull UnarchiveReason reason) {
    log.info("task unarchiveByBoard starts, boardId={}, reason={}", boardId, reason);
    if (reason != UnarchiveReason.CASCADE) {
      throw new IllegalArgumentException("Cannot unarchiveByBoard, reason is incorrect, reason=" + reason);
    }
    boardAccessPort.requireActive(boardId, "unarchiveByBoard");

    List<Task> tasks = taskRepository
            .findTasksByBoardIdWithPessimisticWriteLock(boardId);

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

      log.debug("task with id={} unarchived by board and saved in database", task.getId());
    }

    log.debug("TaskService unarchiveByBoard completed, boardId={}, reason={}", boardId, reason);
  }


  // TODO: убрать task.checkIsActive и оставить эту работу чисто на доменную сущность?
  // TODO: убрать task.checkIsActive и оставить эту работу чисто на доменную сущность?
  // TODO: убрать task.checkIsActive и оставить эту работу чисто на доменную сущность?
  @Transactional
  @Override
  public TaskDto changeName(@NotNull UUID taskId, @NotNull String newName, @NotNull UUID memberId) {
    Task task = getTask(taskId, "changeName");
    task.checkIsActive("changeName");
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
  public TaskDto changeDescription(@NotNull UUID taskId, @NotNull String newDescription, @NotNull UUID memberId) {
    Task task = getTask(taskId, "changeDescription");
    task.checkIsActive("changeDescription");
    workspaceAccessChecker.check(task.getBoardId(), memberId);

    if (!Objects.equals(task.getDescription(), newDescription)) {
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
  public TaskDto changePriority(
          @NotNull UUID taskId,
          @NotNull TaskPriority newPriority,
          @NotNull UUID memberId
  ) {
    Task task = getTask(taskId, "changePriority");
    task.checkIsActive("changePriority");
    workspaceAccessChecker.check(task.getBoardId(), memberId);

    if (!Objects.equals(task.getPriority(), newPriority)) {
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
  public TaskDto changeColumnId(
          @NotNull UUID taskId,
          @NotNull UUID newColumnId,
          @NotNull UUID memberId
  ) {
    Task task = getTask(taskId, "changeColumn");
    // активен ли Task
    task.checkIsActive("changeColumn");
    // WorkspaceMember с указанным memberId является участником Workspace по Task
    workspaceAccessChecker.check(task.getBoardId(), memberId);
    boardAccessPort.requireKanban(task.getBoardId(), "changeColumnId");

    if (!Objects.equals(task.getColumnId(), newColumnId)) {
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
  public TaskDto changeDeadline(
          @NotNull UUID taskId,
          Instant newDeadline,
          @NotNull UUID memberId
  ) {
    Task task = getTask(taskId, "changeDeadline");

    task.checkIsActive("changeDeadline");
    workspaceAccessChecker.check(task.getBoardId(), memberId);

    if (!Objects.equals(task.getDeadline(), newDeadline)) {
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
  public TaskDto changeStatus(
          @NotNull UUID taskId,
          @NotNull TaskStatus newStatus,
          @NotNull UUID memberId
  ) {
    log.info("TaskService changeStatus starts, taskId={}, newStatus={}, memberId={}",
            taskId, newStatus, memberId);
    Task task = getTask(taskId, "changeStatus");
    task.checkIsActive("changeStatus");

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
  public TaskDto changeAssigneeId(
          @NotNull UUID taskId,
          @NotNull UUID newAssigneeId,
          @NotNull UUID memberId
  ) {
    log.info("TaskService setAssignee starts, taskId={}, workspaceMemberId={}, memberId={}",
            taskId, newAssigneeId, memberId);
    Task task = getTask(taskId, "setAssignee");
    task.checkIsActive("setAssignee");

    if (Objects.equals(newAssigneeId, memberId)) {
      workspaceAccessChecker.check(task.getBoardId(), memberId);
    } else {
      workspaceAccessChecker.checkTwo(task.getBoardId(), newAssigneeId, memberId);
    }

    if (!Objects.equals(task.getAssigneeId(), newAssigneeId)) {
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
  public CommentDto addComment(
          @NotNull UUID taskId,
          @NotNull UUID workspaceMemberId,
          @NotBlank String content
  ) {
    Task task = getTask(taskId, "addComment");
    if (workspaceMemberId == null) {
      throw new IllegalArgumentException("Cannot add comment, workspaceMemberId is null, taskId="
              + task.getId());
    }

    task.checkIsActive("addComment");
    workspaceAccessChecker.check(task.getBoardId(), workspaceMemberId);

    Comment comment = task.addComment(workspaceMemberId, content);
    taskRepository.save(task);

    log.debug("TaskService addComment completed, taskId={}, workspaceMemberId={}, content={}",
            taskId, workspaceMemberId, content);
    return commentMapper.toDto(comment);
  }

  @Transactional
  @Override
  public TagDto addTag(
          @NotNull UUID taskId,
          @NotBlank String name,
          @NotNull TagColor color,
          @NotNull UUID memberId
  ) {
    Task task = getTask(taskId, "addTag");

    task.checkIsActive("addTag");
    workspaceAccessChecker.check(task.getBoardId(), memberId);

    Tag tag = task.addTag(name, color);
    taskRepository.save(task);

    log.debug("TaskService addTag completed, taskId={}, name={}, color={}, memberId={}",
            taskId, name, color, memberId);
    return tagMapper.toDto(tag);
  }

  // TODO добавить пагинацию
  // TODO изменить фильтрацию по Tag (разобраться как работать со внутренней сущностью)
  @Transactional(readOnly = true)
  @Override
  public List<TaskDto> filter(
          @NotNull UUID boardId,
          TaskStatus status,
          UUID assigneeId,
          String tagName,
          @NotNull UUID workspaceMemberId
  ) {
    if (assigneeId != null) {
      workspaceAccessChecker.checkTwo(boardId, workspaceMemberId, assigneeId);
    } else {
      workspaceAccessChecker.check(boardId, workspaceMemberId);
    }

    Specification<Task> spec = Specification.unrestricted();

    spec = spec.and(TaskSpecification.boardContains(boardId));
    if (status != null) spec = spec.and(TaskSpecification.statusContains(status));
    if (assigneeId != null) spec = spec.and(TaskSpecification.assigneeContains(assigneeId));
    if (tagName != null) spec = spec.and(TaskSpecification.tagContains(tagName));

    List<Task> tasks = taskRepository.findAll(spec);

    log.info("TaskService filter completed, boardId={}, status={}, assigneeId={}, tagName={}",
            boardId, status, assigneeId, tagName);
    return tasks.stream().map(taskMapper::toDto).toList();
  }

  @Transactional
  @Override
  public List<AuditEntityDto> getTaskChangesHistory(@NotNull UUID taskId, @NotNull UUID memberId) {
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

}
