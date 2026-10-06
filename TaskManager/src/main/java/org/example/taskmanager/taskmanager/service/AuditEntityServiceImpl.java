package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.response.AuditEntityDto;
import org.example.taskmanager.taskmanager.domain.entity.AuditEntity;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.AuditEntityMapper;
import org.example.taskmanager.taskmanager.repository.AuditEntityRepository;
import org.example.taskmanager.taskmanager.service.interfaces.AuditEntityService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditEntityServiceImpl implements AuditEntityService {
  private final AuditEntityRepository auditEntityRepository;
  private final AuditEntityMapper mapper;

  public AuditEntityServiceImpl(
          AuditEntityRepository auditEntityRepository,
          AuditEntityMapper mapper
  ) {
    this.auditEntityRepository = auditEntityRepository;
    this.mapper = mapper;
  }

  @Override
  public void create(UUID taskId, UUID userId, String fieldName, String oldValue, String newValue) {
    auditEntityRepository.save(new AuditEntity(taskId, userId, fieldName, oldValue, newValue));
  }

  @Override
  public List<AuditEntityDto> getTaskChangesHistory(UUID taskId) {
    List<AuditEntity> entities = auditEntityRepository.findAllByTaskIdOrderByTimestampAsc(taskId);

    if (entities.isEmpty())
      throw new NotFoundException("History of task changes with taskId " + taskId + " is empty");

    return entities.stream().map(mapper::toDto).toList();

  }
}
