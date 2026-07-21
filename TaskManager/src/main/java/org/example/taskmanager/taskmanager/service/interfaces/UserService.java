package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.CreateUserRequestDto;
import org.example.taskmanager.taskmanager.domain.entity.User;

import java.util.UUID;

public interface UserService {
  void createUser(CreateUserRequestDto requestDto);
  void deleteUser(UUID id);
}
