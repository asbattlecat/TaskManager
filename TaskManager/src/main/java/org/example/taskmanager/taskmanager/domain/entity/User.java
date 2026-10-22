package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.aggregate.AggregateRoot;
import org.example.taskmanager.taskmanager.domain.event.UserActivatedEvent;
import org.example.taskmanager.taskmanager.domain.event.UserDeactivatedEvent;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Getter
@NoArgsConstructor
@Validated
@Entity
@Table(name = "user")
public class User extends AggregateRoot {
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

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column
  private Instant updatedAt = null;

  public User(
          @NotBlank String name,
          @NotBlank String email,
          @NotBlank String hashedPassword,
          @NotNull boolean active
  ) {
    this.id = UUID.randomUUID();

    this.email = email;
    this.hashedPassword = hashedPassword;
    this.name = name;
    this.active = active;

    this.createdAt = Instant.now();
  }

  // ========= активация/деактивация =================

  public void deactivate() {
    requireActive("deactivate");

    this.active = false;

    touch();
    log.debug("user with deactivated, id={}", id);
//    registerEvent(new UserDeactivatedEvent(id));
  }

  public void activate() {
    if (active) {
      throw new IllegalStateException("Cannot activate user with id=" + id + ", already active");
    }
    this.active = true;

    touch();
    log.debug("user with activated, id={}", id);
//    registerEvent(new UserActivatedEvent(id));
  }

  // ========== изменение полей ===========

  public void changeName(@NotBlank String newName) {
    if (Objects.equals(this.name, newName)) return;
    requireActive("changeName");

    String old = this.name;
    this.name = newName;

    touch();
    log.debug("user changeName completed, id={}, old={}, new={}", id, old, newName);
  }

  public void changeEmail(@NotBlank String newEmail) {
    if (Objects.equals(this.email, newEmail)) return;
    requireActive("changeEmail");

    String old = this.email;
    this.email = newEmail;

    touch();
    log.debug("user changeEmail completed, id={}", id);
  }

  public void changePassword(@NotBlank String newHashedPassword) {
    if (Objects.equals(this.hashedPassword, newHashedPassword)) return;
    requireActive("changePassword");

    this.hashedPassword = newHashedPassword;

    touch();
    log.debug("user changePassword completed, id={}", id);
  }

  // ========== helpers ==========

  private void touch() {
    this.updatedAt = Instant.now();
  }

  private void requireActive(String operationName) {
    if (!active) {
      throw new IllegalStateException("User is not active, cannot do "
              + operationName + ", id=" + id);
    }
  }
}
