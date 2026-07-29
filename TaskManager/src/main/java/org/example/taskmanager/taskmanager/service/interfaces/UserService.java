package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.CreateUserRequest;
import org.example.taskmanager.taskmanager.controller.dto.UpdateUserPasswordRequest;
import org.example.taskmanager.taskmanager.controller.dto.UserDto;

import java.util.UUID;

public interface UserService {
  UserDto create(String name, String email, String password);
  UserDto updateUserInfo(UUID userId, UserDto dto);
  UserDto updatePassword(UUID userId, String oldPassword, String newPassword);
  UserDto activate(UUID userId);
  UserDto deactivate(UUID userId);
  UserDto delete(UUID id);
  UserDto getById(UUID id);
  UserDto getByEmail(String email);
}
