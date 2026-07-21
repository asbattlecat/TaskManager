package org.example.taskmanager.taskmanager.infrastructure.security;

public interface PasswordHasher {
  String encode(CharSequence rawPassword);
  boolean matches(CharSequence rawPassword, String hashedPassword);
}
