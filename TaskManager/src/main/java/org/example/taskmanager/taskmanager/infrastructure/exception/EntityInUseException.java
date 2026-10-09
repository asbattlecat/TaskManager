package org.example.taskmanager.taskmanager.infrastructure.exception;

public class EntityInUseException extends RuntimeException {
  public EntityInUseException(String message) {
    super(message);
  }
}
