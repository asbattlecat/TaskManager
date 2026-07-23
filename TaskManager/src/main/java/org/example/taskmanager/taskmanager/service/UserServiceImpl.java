package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.CreateUserRequestDto;
import org.example.taskmanager.taskmanager.controller.dto.UpdateUserPasswordRequestDto;
import org.example.taskmanager.taskmanager.controller.dto.UserDto;
import org.example.taskmanager.taskmanager.domain.entity.User;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.AlreadyExistsException;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.InvalidCredentialsException;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.security.PasswordHasher;
import org.example.taskmanager.taskmanager.mapper.UserMapper;
import org.example.taskmanager.taskmanager.repository.UserRepository;
import org.example.taskmanager.taskmanager.service.interfaces.UserService;
import org.springframework.stereotype.Service;

import java.util.Optional;
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
  public UserDto create(CreateUserRequestDto requestDto) {
    Optional<User> userOptional = userRepository.findByEmail(requestDto.email());
    if (userOptional.isPresent()) {
      throw new AlreadyExistsException("User with email " + requestDto.email() + " already exists");
    }

    String hashedPassword = passwordHasher.encode(requestDto.password());
    User user = new User(requestDto.name(), requestDto.email(), hashedPassword, true);
    userRepository.save(user);

    return userMapper.toDto(user);
  }

  @Override
  public UserDto updateUserInfo(UUID userId, UserDto dto) {
    Optional<User> userOptional = userRepository.findById(userId);
    checkUserExists(userOptional, userId);
    User user = userOptional.get();

    user.setName(dto.name());
    user.setEmail(dto.email());
    user.setActive(dto.active());

    userRepository.save(user);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto updatePassword(UUID userId, UpdateUserPasswordRequestDto request) {
    Optional<User>  userOptional = userRepository.findById(userId);
    checkUserExists(userOptional, userId);

    User user = userOptional.get();
    String hashedOldPassword = passwordHasher.encode(request.oldPassword());
    if (!passwordHasher.matches(user.getHashedPassword(), hashedOldPassword)) {
      throw new InvalidCredentialsException("Invalid credentials");
    }

    return null;
  }

  @Override
  public UserDto activate(UUID userId) {
    Optional<User>  userOptional = userRepository.findById(userId);
    checkUserExists(userOptional, userId);

    User user = userOptional.get();
    if (user.isActive()) {
      throw new  InvalidCredentialsException("User already activated");
    }

    user.setActive(true);
    userRepository.save(user);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto deactivate(UUID userId) {
    Optional<User>  userOptional = userRepository.findById(userId);
    checkUserExists(userOptional, userId);

    User user = userOptional.get();
    if (!user.isActive()) {
      throw new  InvalidCredentialsException("User already deactivated");
    }

    user.setActive(false);
    userRepository.save(user);
    return userMapper.toDto(user);
  }

  @Override
  public UserDto delete(UUID id) {
    Optional<User> userOptional = userRepository.findById(id);
    checkUserExists(userOptional, id);

    userRepository.deleteById(id);
    return userMapper.toDto(userOptional.get());
  }

  @Override
  public UserDto getById(UUID id) {
    Optional<User> userOptional = userRepository.findById(id);
    checkUserExists(userOptional, id);

    return userMapper.toDto(userOptional.get());
  }

  @Override
  public UserDto getByEmail(String email) {
    Optional<User> userOptional = userRepository.findByEmail(email);
    checkUserExists(userOptional, email);

    return userMapper.toDto(userOptional.get());
  }

  private void checkUserExists(Optional<User> userOptional, UUID id) {
    if (userOptional.isEmpty()) {
      throw new NotFoundException("User with id " + id + " not found");
    }
  }

  private void checkUserExists(Optional<User> userOptional, String email) {
    if (userOptional.isEmpty()) {
      throw new NotFoundException("User with email " + email + " not found");
    }
  }
}
