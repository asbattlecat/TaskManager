package org.example.taskmanager.taskmanager.mapper;

import org.example.taskmanager.taskmanager.controller.dto.response.TaskDto;
import org.example.taskmanager.taskmanager.domain.aggregate.Task;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapper {
  TaskDto toDto(Task task);
  Task toEntity(TaskDto taskDto);
}
