package org.example.taskmanager.taskmanager.mapper;

import org.example.taskmanager.taskmanager.controller.dto.AuditEntityDto;
import org.example.taskmanager.taskmanager.domain.entity.AuditEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditEntityMapper {
  AuditEntityDto toDto(AuditEntity auditEntity);
  AuditEntity toEntity(AuditEntityDto auditEntityDto);
}
