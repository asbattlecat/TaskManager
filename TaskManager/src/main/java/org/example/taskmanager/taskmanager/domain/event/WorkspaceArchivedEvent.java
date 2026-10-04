package org.example.taskmanager.taskmanager.domain.event;

import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEvent;

import java.util.UUID;

public record WorkspaceArchivedEvent(UUID workspaceId, ArchiveReason reason) implements DomainEvent {
}
