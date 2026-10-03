package org.example.taskmanager.taskmanager.controller.dto.event;

import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEvent;

import java.util.UUID;

public record BoardArchived(UUID boardId, ArchiveReason reason) implements DomainEvent {
}
