package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.AuditEntityDto;
import org.example.taskmanager.taskmanager.domain.entity.AuditEntry;
import org.example.taskmanager.taskmanager.mapper.AuditEntityMapper;
import org.example.taskmanager.taskmanager.repository.AuditEntityRepository;
import org.example.taskmanager.taskmanager.service.interfaces.AuditEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  @Transactional
  @Override
  public void create(UUID taskId, UUID userId, String fieldName, String oldValue, String newValue) {
    AuditEntry auditEntry = new AuditEntry(taskId, userId, fieldName, oldValue, newValue);
    auditEntityRepository.save(auditEntry);

    log.debug("audit entity created and saved in database, auditEntityId={}", auditEntry.getId());
  }

  @Transactional
  @Override
  public List<AuditEntityDto> getTaskChangesHistory(UUID taskId) {
    List<AuditEntry> entities = auditEntityRepository.findAllByTaskIdOrderByTimestampAsc(taskId);

    log.debug("audit entity getTaskChangesHistory operation completed, taskId={}", taskId);
    return entities.stream().map(mapper::toDto).toList();
  }
}
