package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.CreateUserRequestDto;
import org.example.taskmanager.taskmanager.domain.entity.User;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.AlreadyExistsException;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.security.PasswordHasher;
import org.example.taskmanager.taskmanager.repository.UserRepository;
import org.example.taskmanager.taskmanager.service.interfaces.UserService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;

  public UserServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
  }


  @Override
  public void createUser(CreateUserRequestDto requestDto) {
    Optional<User> userOptional = userRepository.findByEmail(requestDto.email());
    if (userOptional.isPresent()) {
      throw new AlreadyExistsException("User with email " + requestDto.email() + " already exists");
    }

    String hashedPassword = passwordHasher.encode(requestDto.password());
    User user = new User(requestDto.name(), requestDto.email(), hashedPassword, true);
    userRepository.save(user);
  }

  @Override
  public void deleteUser(UUID id) {
    Optional<User> userOptional = userRepository.findById(id);
    if (userOptional.isEmpty()) {
      throw new NotFoundException("User with id " + id + " not found");
    }

    userRepository.deleteById(id);
  }
}
