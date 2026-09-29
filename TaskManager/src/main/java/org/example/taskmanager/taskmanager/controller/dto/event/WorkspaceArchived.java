package org.example.taskmanager.taskmanager.controller.dto.event;

import org.example.taskmanager.taskmanager.domain.enums.ArchiveReason;

import java.util.UUID;

public record WorkspaceArchived(UUID workspaceId, ArchiveReason reason) implements DomainEvent {
}
