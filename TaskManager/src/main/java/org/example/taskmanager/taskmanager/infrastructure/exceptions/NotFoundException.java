package org.example.taskmanager.taskmanager.infrastructure.exceptions;

public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }
}
