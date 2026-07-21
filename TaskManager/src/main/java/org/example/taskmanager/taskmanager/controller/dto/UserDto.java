package org.example.taskmanager.taskmanager.controller.dto;

import java.util.UUID;

public record UserDto(UUID id, String name, String email, boolean active) {
}
