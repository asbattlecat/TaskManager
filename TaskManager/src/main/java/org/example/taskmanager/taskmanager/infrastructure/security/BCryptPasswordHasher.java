package org.example.taskmanager.taskmanager.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class BCryptPasswordHasher implements PasswordEncoder {
  private final PasswordEncoder encoder =  new BCryptPasswordEncoder();

  @Override
  public String encode(CharSequence rawPassword) {
    return encoder.encode(rawPassword);
  }

  @Override
  public boolean matches(CharSequence rawPassword, String hashedPassword) {
    return encoder.matches(rawPassword, hashedPassword);
  }
}
