package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class User {
  @Id
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  @Email
  private String email;

  @Column(nullable = false)
  private String hashedPassword;

  @Column(nullable = false)
  private boolean active;

  public User(@NotNull String name, @NotNull String email, @NotNull String hashedPassword, @NotNull boolean active) {
    this.id = UUID.randomUUID();

    this.email = email;
    this.hashedPassword = hashedPassword;
    this.name = name;
    this.active = active;
  }

  public void deactivate() {
    this.active = false;
  }

  public void activate() {
    this.active = true;
  }
}
