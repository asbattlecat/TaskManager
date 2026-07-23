package org.example.taskmanager.taskmanager.controller.dto;

import java.util.UUID;

public record UpdateUserPasswordRequestDto(String oldPassword, String newPassword) {
}
