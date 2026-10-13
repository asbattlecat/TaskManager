package org.example.taskmanager.taskmanager.mapper;

import org.example.taskmanager.taskmanager.controller.dto.response.AuditEntityDto;
import org.example.taskmanager.taskmanager.domain.entity.AuditEntry;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditEntityMapper {
  AuditEntityDto toDto(AuditEntry auditEntry);
  AuditEntry toEntity(AuditEntityDto auditEntityDto);
}
