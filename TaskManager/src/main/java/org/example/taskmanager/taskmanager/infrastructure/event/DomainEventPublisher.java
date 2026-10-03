package org.example.taskmanager.taskmanager.infrastructure.event;

public interface DomainEventPublisher {
  void publish(DomainEvent event);
}
