package org.example.taskmanager.taskmanager.domain.event;

import org.example.taskmanager.taskmanager.infrastructure.event.DomainEvent;

import java.util.UUID;

public record UserActivatedEvent(UUID userId) implements DomainEvent {
}
