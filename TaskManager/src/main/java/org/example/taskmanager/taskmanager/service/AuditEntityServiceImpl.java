package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.AuditEntityDto;
import org.example.taskmanager.taskmanager.domain.entity.AuditEntity;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.logs.CustomLogger;
import org.example.taskmanager.taskmanager.mapper.AuditEntityMapper;
import org.example.taskmanager.taskmanager.repository.AuditEntityRepository;
import org.example.taskmanager.taskmanager.service.interfaces.AuditEntityService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
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
    log.info("creating audit entity, taskId={}, userId={}, fieldName={}, oldValue={}, newValue={}",
            taskId, userId, fieldName, oldValue, newValue);
    AuditEntity auditEntity = new AuditEntity(taskId, userId, fieldName, oldValue, newValue);
    auditEntityRepository.save(auditEntity);

    log.debug("audit entity created and saved in database, auditEntityId={}", auditEntity.getId());
  }

  @Override
  public List<AuditEntityDto> getTaskChangesHistory(UUID taskId) {
    CustomLogger.operationStarts("audit entity", "getTaskChangesHistory", "taskId", taskId);

    List<AuditEntity> entities = auditEntityRepository.findAllByTaskIdOrderByTimestampAsc(taskId);

    CustomLogger.operationCompleted("audit entity", "getTaskChangesHistory", "taskId", taskId);
    return entities.stream().map(mapper::toDto).toList();
  }
}
