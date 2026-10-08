package org.example.taskmanager.taskmanager.infrastructure.logs;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class CustomLogger {
  public static void cascadeOperationStarts(String operation, String entities, String forWhat, UUID id) {
    log.info("Starting cascade {} of {} for {} {}", operation, entities, forWhat, id);
  }

  public static void cascadeOperationComplete(String operation, String entites, String forWhat, UUID id) {
    log.info("Cascade {} of {} complete for {} {}", operation, entites, forWhat, id);
  }

  public static void cascadeOperationFailed(String operation, String entites, String forWhat, UUID id) {
    log.error("Cascade {} of {} failed for {} {}", operation, entites, forWhat, id);
  }

  public static void operationStarts(String entity, String operationName, String entityId, UUID id) {
    log.info("{} {} starts, {}={}", entity, operationName, entityId, id);
  }

  public static void operationCompleted(String entity, String operationName, String entityId, UUID id) {
    log.debug("{} {} completed, {}={}", entity, operationName, entityId, id);
  }
}
