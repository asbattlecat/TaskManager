package org.example.taskmanager.taskmanager.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class BCryptPasswordHasher implements PasswordHasher {
  private final PasswordEncoder encoder = new BCryptPasswordEncoder();

  @Override
  public String encode(CharSequence rawPassword) {
    return encoder.encode(rawPassword);
  }

  @Override
  public boolean matches(CharSequence rawPassword, String hashedPassword) {
    return encoder.matches(rawPassword, hashedPassword);
  }
}
