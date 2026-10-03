package org.example.taskmanager.taskmanager.mapper;

import org.example.taskmanager.taskmanager.controller.dto.response.WorkspaceMemberDto;
import org.example.taskmanager.taskmanager.domain.entity.WorkspaceMember;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkspaceMemberMapper {
  WorkspaceMemberDto toDto(WorkspaceMember workspaceMember);
  WorkspaceMember toEntity(WorkspaceMemberDto dto);
}
