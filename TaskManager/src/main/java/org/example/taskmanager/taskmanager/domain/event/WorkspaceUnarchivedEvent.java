package org.example.taskmanager.taskmanager.domain.event;

import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEvent;

import java.util.UUID;

public record WorkspaceUnarchivedEvent(UUID workspaceId, UnarchiveReason unarchiveReason) implements DomainEvent {
}
