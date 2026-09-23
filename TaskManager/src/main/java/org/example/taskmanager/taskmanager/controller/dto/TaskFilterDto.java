package org.example.taskmanager.taskmanager.controller.dto;

import org.example.taskmanager.taskmanager.domain.entity.Tag;
import org.example.taskmanager.taskmanager.domain.enums.TaskStatus;

import java.util.UUID;

public record TaskFilterDto(
        TaskStatus status,
        UUID assigneeId,
        Tag tag
) {
}
