package org.example.taskmanager.taskmanager.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserPasswordRequest(@NotNull String oldPassword,
                                        @NotNull String newPassword) {
}
