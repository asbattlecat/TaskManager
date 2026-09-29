package org.example.taskmanager.taskmanager.infrastructure.event;

import org.example.taskmanager.taskmanager.controller.dto.event.DomainEvent;

public interface DomainEventPublisher {
  void publish(DomainEvent event);
}
