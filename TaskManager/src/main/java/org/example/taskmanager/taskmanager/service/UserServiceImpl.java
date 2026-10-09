package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.UserDto;
import org.example.taskmanager.taskmanager.domain.entity.User;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;
import org.example.taskmanager.taskmanager.infrastructure.exception.AlreadyExistsException;
import org.example.taskmanager.taskmanager.infrastructure.exception.EntityInUseException;
import org.example.taskmanager.taskmanager.infrastructure.exception.InvalidCredentialsException;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.logs.CustomLogger;
import org.example.taskmanager.taskmanager.infrastructure.security.PasswordHasher;
import org.example.taskmanager.taskmanager.mapper.UserMapper;
import org.example.taskmanager.taskmanager.repository.UserRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceMemberRepository;
import org.example.taskmanager.taskmanager.service.interfaces.UserService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final UserMapper userMapper;

  public UserServiceImpl(
          WorkspaceMemberRepository workspaceMemberRepository,
          UserRepository userRepository,
          PasswordHasher passwordHasher,
          UserMapper userMapper
  ) {
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
    this.userMapper = userMapper;
  }

  @Override
  public UserDto create(String name, String email, String password) {
    log.info("creating user, name={}, email={}", name, email);

    if (userRepository.existsByEmail(email)) {
      log.error("user not created, already exists, email={}", email);
      throw new AlreadyExistsException("User with email " + email + " already exists");
    }

    String hashedPassword = passwordHasher.encode(password);
    User user = new User(name, email, hashedPassword, true);
    userRepository.save(user);

    log.debug("user created, userId={}", user.getId());
    return userMapper.toDto(user);
  }

  @Override
  public UserDto updateUserInfo(UUID userId, UserDto dto) {
    CustomLogger.operationStarts("user", "updateUserInfo", "userId", userId);
    User user = getUser(userId);

    user.setName(dto.name());
    user.setEmail(dto.email());
    user.setActive(dto.active());

    userRepository.save(user);

    CustomLogger.operationCompleted("user", "updateUserInfo", "userId", userId);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto updatePassword(UUID userId, String oldPassword, String newPassword) {
    CustomLogger.operationStarts("user", "updatePassword", "userId", userId);
    User user = getUser(userId);

    if (!passwordHasher.matches(oldPassword, user.getHashedPassword())) {
      log.error("password was not updated, invalid credentials");
      throw new InvalidCredentialsException("Invalid credentials");
    }

    String hashedNewPassword = passwordHasher.encode(newPassword);

    user.setHashedPassword(hashedNewPassword);
    userRepository.save(user);

    CustomLogger.operationCompleted("user", "updatePassword", "userId", userId);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto activate(UUID userId) {
    CustomLogger.operationStarts("user", "activate", "userId", userId);
    User user = getUser(userId);

    if (user.isActive()) {
      log.error("user is already active, userId={}", userId);
      throw new  InvalidCredentialsException("User with userId " + userId + " is already active");
    }

    user.setActive(true);
    userRepository.save(user);

    CustomLogger.operationCompleted("user", "activate", "userId", userId);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto deactivate(UUID userId) {
    CustomLogger.operationStarts("user", "deactivate", "userId", userId);
    User user = getUser(userId);

    if (!user.isActive()) {
      log.error("user is already deactivated, userId={}", userId);
      throw new  InvalidCredentialsException("User with userId " + userId + " is already deactivated");
    }

    user.setActive(false);
    userRepository.save(user);
    CustomLogger.operationCompleted("user", "deactivate", "userId", userId);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto delete(UUID id) {
    CustomLogger.operationStarts("user", "delete", "userId", id);

    if (workspaceMemberRepository.existsByUserIdAndRole(id, WorkspaceRole.ADMIN)) {
      log.error("user not deleted, he is admin in workspace, userId={}", id);
      throw new EntityInUseException("Cannot delete user with id " + id
              + " because this user is admin in workspace");
    }
    User user = getUser(id);

    userRepository.delete(user);
    CustomLogger.operationCompleted("user", "delete", "userId", id);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto getById(UUID id) {
    CustomLogger.operationStarts("user", "getById", "userId", id);
    User user = getUser(id);

    CustomLogger.operationCompleted("user", "getById", "userId", id);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto getByEmail(String email) {
    log.info("user getByEmail starts, email={}", email);

    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
              log.error("user not found, email={}", email);
              return new NotFoundException("User with email " + email + " not found");
            });

    log.debug("user getByEmail completed, email={}", email);
    return userMapper.toDto(user);
  }

  private User getUser(UUID userId) {
    return userRepository.findById(userId)
            .orElseThrow(() -> {
              log.error("user not found, userId={}", userId);
              return new NotFoundException("User with userId " + userId + " not found");
            });
  }
}
