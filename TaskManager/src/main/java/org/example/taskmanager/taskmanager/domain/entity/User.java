package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.aggregate.AggregateRoot;
import org.example.taskmanager.taskmanager.domain.event.UserActivatedEvent;
import org.example.taskmanager.taskmanager.domain.event.UserDeactivatedEvent;

import java.util.UUID;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@Entity
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

  public User(@NotNull String name, @NotNull String email, @NotNull String hashedPassword, @NotNull boolean active) {
    this.id = UUID.randomUUID();

    this.email = email;
    this.hashedPassword = hashedPassword;
    this.name = name;
    this.active = active;
  }

  public void deactivate() {
    if (!active) {
      log.debug("cannot deactivate user with id={}, already deactivated", id);
      throw new IllegalStateException("Cannot deactivate user with id=" + id + ", already deactivated");
    }
    this.active = false;
    log.debug("user with id={} deactivated", id);

    registerEvent(new UserDeactivatedEvent(id));
  }

  public void activate() {
    if (active) {
      log.debug("cannot activate user with id={}, already active", id);
      throw new IllegalStateException("Cannot activate user with id=" + id + ", already active");
    }
    this.active = true;
    log.debug("user with id={} activated", id);

    registerEvent(new UserActivatedEvent(id));
  }
}
