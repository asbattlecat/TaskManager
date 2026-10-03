package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.response.UserDto;
import org.example.taskmanager.taskmanager.domain.entity.User;
import org.example.taskmanager.taskmanager.infrastructure.exception.AlreadyExistsException;
import org.example.taskmanager.taskmanager.infrastructure.exception.InvalidCredentialsException;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.security.PasswordHasher;
import org.example.taskmanager.taskmanager.mapper.UserMapper;
import org.example.taskmanager.taskmanager.repository.UserRepository;
import org.example.taskmanager.taskmanager.service.interfaces.UserService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final UserMapper userMapper;

  public UserServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher, UserMapper userMapper) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
    this.userMapper = userMapper;
  }

  @Override
  public UserDto create(String name, String email, String password) {
    if (userRepository.existsByEmail(email)) {
      throw new AlreadyExistsException("User with email " + email + " already exists");
    }

    String hashedPassword = passwordHasher.encode(password);
    User user = new User(name, email, hashedPassword, true);
    userRepository.save(user);

    return userMapper.toDto(user);
  }

  @Override
  public UserDto updateUserInfo(UUID userId, UserDto dto) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

    user.setName(dto.name());
    user.setEmail(dto.email());
    user.setActive(dto.active());

    userRepository.save(user);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto updatePassword(UUID userId, String oldPassword, String newPassword) {
    User user = getUser(userId);

    String hashedOldPassword = passwordHasher.encode(oldPassword);
    if (!passwordHasher.matches(user.getHashedPassword(), hashedOldPassword)) {
      throw new InvalidCredentialsException("Invalid credentials");
    }

    String hashedNewPassword = passwordHasher.encode(newPassword);

    user.setHashedPassword(hashedNewPassword);
    userRepository.save(user);

    return userMapper.toDto(user);
  }

  @Override
  public UserDto activate(UUID userId) {
    User user = getUser(userId);

    if (user.isActive()) {
      throw new  InvalidCredentialsException("User already activated");
    }

    user.setActive(true);
    userRepository.save(user);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto deactivate(UUID userId) {
    User user = getUser(userId);

    if (!user.isActive()) {
      throw new  InvalidCredentialsException("User already deactivated");
    }

    user.setActive(false);
    userRepository.save(user);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto delete(UUID id) {
    User user = getUser(id);

    userRepository.delete(user);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto getById(UUID id) {
    User user = getUser(id);

    return userMapper.toDto(user);
  }

  @Override
  public UserDto getByEmail(String email) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found"));

    return userMapper.toDto(user);
  }

  private User getUser(UUID userId) {
    return userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
