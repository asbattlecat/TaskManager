package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;

public record UserDto(@NotNull String name,
                      @NotNull String email,
                      @NotNull boolean active) {
}
