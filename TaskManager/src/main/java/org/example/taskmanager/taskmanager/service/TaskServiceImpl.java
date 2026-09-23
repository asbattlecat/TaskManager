package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.TaskDto;
import org.example.taskmanager.taskmanager.domain.entity.Tag;
import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.example.taskmanager.taskmanager.domain.enums.TagColor;
import org.example.taskmanager.taskmanager.domain.enums.TaskPriority;
import org.example.taskmanager.taskmanager.domain.enums.TaskStatus;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.TaskMapper;
import org.example.taskmanager.taskmanager.repository.BoardColumnRepository;
import org.example.taskmanager.taskmanager.repository.CommentRepository;
import org.example.taskmanager.taskmanager.repository.TagRepository;
import org.example.taskmanager.taskmanager.repository.TaskRepository;
import org.example.taskmanager.taskmanager.service.interfaces.TaskService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TaskServiceImpl implements TaskService {
  private final TaskRepository taskRepository;
  private final TagRepository tagRepository;
  private final CommentRepository commentRepository;
  private final BoardColumnRepository boardColumnRepository;
  private final TaskMapper taskMapper;

  public  TaskServiceImpl(TaskRepository taskRepository, TagRepository tagRepository,
                          CommentRepository commentRepository, TaskMapper taskMapper,
                          BoardColumnRepository boardColumnRepository) {
    this.taskRepository = taskRepository;
    this.tagRepository = tagRepository;
    this.commentRepository = commentRepository;
    this.taskMapper = taskMapper;
    this.boardColumnRepository = boardColumnRepository;
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
    return null;
  }

  @Override
  public TaskDto setAssignee(UUID taskId, UUID assigneeId) {
    return null;
  }

  @Override
  public void addComment(UUID taskId, UUID authorId, String content) {

  }

  @Override
  public void addTag(UUID taskId, String name, TagColor color) {

  }

  @Override
  public List<TaskDto> filter(TaskStatus status, UUID assigneeId, Tag tag) {
    return List.of();
  }

  private Task getTask(UUID taskId) {
    return taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException("Task not found"));
  }
}
