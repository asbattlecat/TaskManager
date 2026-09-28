package org.example.taskmanager.taskmanager.infrastructure.exception;

public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }
}
