package org.example.taskmanager.taskmanager.domain.aggregate;

import org.example.taskmanager.taskmanager.infrastructure.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class AggregateRoot {
  private final List<DomainEvent> events = new ArrayList<>();

  public void registerEvent(DomainEvent event) {
    events.add(event);
  }

  public List<DomainEvent> getEvents() {
    return List.copyOf(events);
  }

  public void clearEvents() {
    events.clear();
  }
}
