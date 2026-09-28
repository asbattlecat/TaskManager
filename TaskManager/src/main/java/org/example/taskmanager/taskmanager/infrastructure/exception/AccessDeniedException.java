package org.example.taskmanager.taskmanager.infrastructure.exception;

public class AccessDeniedException extends RuntimeException {
  public AccessDeniedException(String message) {
    super(message);
  }
}
