package org.example.taskmanager.taskmanager.service.interfaces;

import org.example.taskmanager.taskmanager.controller.dto.CreateUserRequestDto;
import org.example.taskmanager.taskmanager.domain.entity.User;

public interface UserService {
  void createUser(CreateUserRequestDto requestDto);

}
