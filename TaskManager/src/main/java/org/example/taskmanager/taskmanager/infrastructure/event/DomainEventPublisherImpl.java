package org.example.taskmanager.taskmanager.infrastructure.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisherImpl implements DomainEventPublisher {
  private final ApplicationEventPublisher eventPublisher;

  public DomainEventPublisherImpl(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void publish(DomainEvent event) {
    eventPublisher.publishEvent(event);
  }
}
