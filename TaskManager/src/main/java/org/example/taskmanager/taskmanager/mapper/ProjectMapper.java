package org.example.taskmanager.taskmanager.mapper;

import org.example.taskmanager.taskmanager.controller.dto.response.ProjectDto;
import org.example.taskmanager.taskmanager.domain.aggregate.Project;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
  ProjectDto toDto(Project project);
  Project toEntity(ProjectDto projectDto);
}
