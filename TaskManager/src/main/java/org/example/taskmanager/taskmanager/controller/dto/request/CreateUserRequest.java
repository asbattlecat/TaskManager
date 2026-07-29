package org.example.taskmanager.taskmanager.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(@NotNull String name,
                                @NotNull String email,
                                @NotNull String password) {
}
