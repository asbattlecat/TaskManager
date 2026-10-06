package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.response.AuditEntityDto;

import java.util.List;
import java.util.UUID;

public interface AuditEntityService {
  void create(UUID taskId, UUID userId, String fieldName, String oldValue, String newValue);
  List<AuditEntityDto> getTaskChangesHistory(UUID taskId);
}
