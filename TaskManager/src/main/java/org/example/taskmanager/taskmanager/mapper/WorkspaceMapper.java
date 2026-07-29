package org.example.taskmanager.taskmanager.mapper;

import org.example.taskmanager.taskmanager.controller.dto.WorkspaceDto;
import org.example.taskmanager.taskmanager.domain.entity.Workspace;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {
  WorkspaceDto toDto(Workspace workspace);
  Workspace toEntity(WorkspaceDto dto);
}
