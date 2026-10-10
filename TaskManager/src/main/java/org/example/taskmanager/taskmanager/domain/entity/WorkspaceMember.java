package org.example.taskmanager.taskmanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceMemberState;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceMember {
  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID workspaceId;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private WorkspaceMemberState state;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private WorkspaceRole role;

  @Column(nullable = false)
  private Instant joinedAt;

  public WorkspaceMember(@NotNull UUID workspaceId, @NotNull UUID userId, @NotNull WorkspaceRole role) {
    id = UUID.randomUUID();

    this.workspaceId = workspaceId;
    this.userId = userId;
    state = WorkspaceMemberState.ACTIVE;
    this.role = role;

    this.joinedAt = Instant.now();
  }

  public void archive() {
    if (state == WorkspaceMemberState.USER_ARCHIVED) {
      log.error("cannot archive member, already archived, id={}", id);
      throw new IllegalStateException("Cannot archive member, already archived, id=" + id);
    }
    if (state == WorkspaceMemberState.MANAGER_BLOCKED) {
      log.error("cannot archive member, blocked by manager, id={}", id);
      throw new IllegalStateException("Cannot archive member, blocked by manager, id=" + id);
    }
    state = WorkspaceMemberState.USER_ARCHIVED;
    log.debug("workspace member archived, id={}", id);
  }

  public void unarchive() {
    if (state == WorkspaceMemberState.ACTIVE) {
      log.error("cannot unarchive member, already active, id={}", id);
      throw new IllegalStateException("Cannot unarchive member, already active, id=" + id);
    }
    if (state == WorkspaceMemberState.MANAGER_BLOCKED) {
      log.error("cannot unarchive member, blocked by manager, id={}", id);
      throw new IllegalStateException("Cannot unarchive member, blocked by manager, id=" + id);
    }
    state = WorkspaceMemberState.ACTIVE;
    log.debug("workspace member unarchived, id={}", id);
  }

  public void block() {
    if (state == WorkspaceMemberState.MANAGER_BLOCKED) {
      log.error("cannot block member, already blocked, id={}", id);
      throw new IllegalStateException("Cannot block member, already blocked, id=" + id);
    }
    state = WorkspaceMemberState.MANAGER_BLOCKED;
    log.debug("workspace member blocked, id={}", id);
  }

  public void unblock() {
    if (state == WorkspaceMemberState.ACTIVE) {
      log.error("cannot unbluck member, already active, id={}", id);
      throw new IllegalStateException("Cannot unblock member, already active, id=" + id);
    }
    state = WorkspaceMemberState.ACTIVE;
    log.debug("workspace member unblocked, id={}", id);
  }
}
