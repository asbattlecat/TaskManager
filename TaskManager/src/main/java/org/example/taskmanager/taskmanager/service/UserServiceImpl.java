package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.UserDto;
import org.example.taskmanager.taskmanager.domain.entity.User;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;
import org.example.taskmanager.taskmanager.infrastructure.event.DomainEventPublisher;
import org.example.taskmanager.taskmanager.infrastructure.exception.AlreadyExistsException;
import org.example.taskmanager.taskmanager.infrastructure.exception.EntityInUseException;
import org.example.taskmanager.taskmanager.infrastructure.exception.InvalidCredentialsException;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
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
  private final DomainEventPublisher domainEventPublisher;
  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final UserMapper userMapper;

  public UserServiceImpl(
          WorkspaceMemberRepository workspaceMemberRepository,
          DomainEventPublisher domainEventPublisher,
          UserRepository userRepository,
          PasswordHasher passwordHasher,
          UserMapper userMapper
  ) {
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.domainEventPublisher = domainEventPublisher;
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
    log.info("user updateUserInfo operation starts, userId={}, userName={}, userEmail={}, userActive={}",
            userId, dto.name(), dto.email(), dto.active());
    User user = getUser(userId);

    user.setName(dto.name());
    user.setEmail(dto.email());
    user.setActive(dto.active());

    userRepository.save(user);

    log.debug("user updateUserInfo operation completed, userId={}, userName={}, userEmail={}, userActive={}",
            userId, dto.name(), dto.email(), dto.active());
    return userMapper.toDto(user);
  }

  @Override
  public UserDto updatePassword(UUID userId, String oldPassword, String newPassword) {
    log.info("user updatePassword operation starts, userId={}", userId);
    User user = getUser(userId);

    if (!passwordHasher.matches(oldPassword, user.getHashedPassword())) {
      log.error("password was not updated, invalid credentials");
      throw new InvalidCredentialsException("Invalid credentials");
    }

    String hashedNewPassword = passwordHasher.encode(newPassword);

    user.setHashedPassword(hashedNewPassword);
    userRepository.save(user);

    log.debug("user updatePassword operation completed, userId={}", userId);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto activate(UUID userId) {
    log.info("user activate operation starts, userId={}", userId);
    User user = getUser(userId);

    user.activate();
    userRepository.save(user);
    user.getEvents().forEach(domainEventPublisher::publish);
    user.clearEvents();

    log.debug("user activate operation completed, userId={}", userId);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto deactivate(UUID userId) {
    log.info("user deactivate operation starts, userId={}", userId);
    User user = getUser(userId);

    user.deactivate();
    userRepository.save(user);
    user.getEvents().forEach(domainEventPublisher::publish);
    user.clearEvents();

    log.debug("user deactivate operation completed, userId={}", userId);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto delete(UUID userId) {
    log.info("user delete operation starts, userId={}", userId);

    if (workspaceMemberRepository.existsByUserIdAndRole(userId, WorkspaceRole.ADMIN)) {
      log.error("user not deleted, he is admin in workspace, userId={}", userId);
      throw new EntityInUseException("Cannot delete user with id " + userId
              + " because this user is admin in workspace");
    }
    User user = getUser(userId);

    userRepository.delete(user);
    log.debug("user delete operation completed, userId={}", userId);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto getById(UUID userId) {
    log.info("user getById operation starts, userId={}", userId);
    User user = getUser(userId);

    log.debug("user getById operation completed, userId={}", userId);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto getByEmail(String email) {
    log.info("user getByEmail operation starts, email={}", email);

    User user = userRepository
            .findByEmail(email)
            .orElseThrow(() -> {
              log.error("user not found, email={}", email);
              return new NotFoundException("User with email " + email + " not found");
            });

    log.debug("user getByEmail operation completed, email={}", email);
    return userMapper.toDto(user);
  }

  private User getUser(UUID userId) {
    return userRepository
            .findById(userId)
            .orElseThrow(() -> {
              log.error("user not found, userId={}", userId);
              return new NotFoundException("User with userId " + userId + " not found");
            });
  }
}
