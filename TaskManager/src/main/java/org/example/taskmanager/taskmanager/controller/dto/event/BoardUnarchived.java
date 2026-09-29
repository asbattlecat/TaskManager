package org.example.taskmanager.taskmanager.controller.dto.event;

import org.example.taskmanager.taskmanager.domain.enums.UnarchiveReason;

import java.util.UUID;

public record BoardUnarchived(UUID boardId, UnarchiveReason unarchiveReason) implements DomainEvent {
}
