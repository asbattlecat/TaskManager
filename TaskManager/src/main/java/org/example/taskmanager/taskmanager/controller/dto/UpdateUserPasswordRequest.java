package org.example.taskmanager.taskmanager.controller.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserPasswordRequest(@NotNull String oldPassword,
                                        @NotNull String newPassword) {
}
