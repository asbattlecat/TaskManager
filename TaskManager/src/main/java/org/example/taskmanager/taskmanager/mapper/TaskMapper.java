package org.example.taskmanager.taskmanager.mapper;

import org.example.taskmanager.taskmanager.controller.dto.TaskDto;
import org.example.taskmanager.taskmanager.domain.entity.Task;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapper {
  TaskDto toDto(Task task);
  Task toEntity(TaskDto taskDto);
}
