package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.CreateUserRequestDto;
import org.example.taskmanager.taskmanager.controller.dto.UpdateUserPasswordRequestDto;
import org.example.taskmanager.taskmanager.controller.dto.UserDto;

import java.util.UUID;

public interface UserService {
  UserDto create(CreateUserRequestDto requestDto);
  UserDto updateUserInfo(UUID userId, UserDto dto);
  UserDto updatePassword(UUID userId, UpdateUserPasswordRequestDto request);
  UserDto activate(UUID userId);
  UserDto deactivate(UUID userId);
  UserDto delete(UUID id);
  UserDto getById(UUID id);
  UserDto getByEmail(String email);
}
