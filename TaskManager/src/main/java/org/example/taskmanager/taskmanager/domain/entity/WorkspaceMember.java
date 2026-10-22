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
@NoArgsConstructor
public class WorkspaceMember {
  @Id
  private UUID id;

  @Column(nullable = false, updatable = false)
  private UUID workspaceId;

  @Column(nullable = false, updatable = false)
  private UUID userId;

  @Column(nullable = false)
  private WorkspaceMemberState state;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private WorkspaceRole role;

  @Column(nullable = false, updatable = false)
  private Instant joinedAt;

  @Column
  private Instant updatedAt;

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
      throw new IllegalStateException("Cannot archive member, already archived, id=" + id);
    }
    if (state == WorkspaceMemberState.MANAGER_BLOCKED) {
      throw new IllegalStateException("Cannot archive member, blocked by manager, id=" + id);
    }
    state = WorkspaceMemberState.USER_ARCHIVED;

    touch();
    log.debug("workspace member archived, id={}", id);
  }

  public void unarchive() {
    if (state == WorkspaceMemberState.ACTIVE) {
      throw new IllegalStateException("Cannot unarchive member, already active, id=" + id);
    }
    if (state == WorkspaceMemberState.MANAGER_BLOCKED) {
      throw new IllegalStateException("Cannot unarchive member, blocked by manager, id=" + id);
    }
    state = WorkspaceMemberState.ACTIVE;

    touch();
    log.debug("workspace member unarchived, id={}", id);
  }

  public void block() {
    if (state == WorkspaceMemberState.MANAGER_BLOCKED) {
      throw new IllegalStateException("Cannot block member, already blocked, id=" + id);
    }

    state = WorkspaceMemberState.MANAGER_BLOCKED;
    touch();
    log.debug("workspace member blocked, id={}", id);
  }

  public void unblock() {
    if (state == WorkspaceMemberState.ACTIVE) {
      throw new IllegalStateException("Cannot unblock member, already active, id=" + id);
    }

    state = WorkspaceMemberState.ACTIVE;
    touch();
    log.debug("workspace member unblocked, id={}", id);
  }

  // ============== изменение полей ==================

  public void changeRole(WorkspaceRole newRole) {
    if (this.role == newRole) return;
    requireActive("changeRole");

    WorkspaceRole old = role;
    this.role = newRole;

    touch();
    log.debug("workspace changeRole completed, id={}, old={}, new={}", id, old, newRole);
  }

  // ================= helpers ================

  private void requireActive(String operationName) {
    if (state != WorkspaceMemberState.ACTIVE)
      throw new IllegalStateException("Cannot do , workspace member is not active, id=" + id);
  }

  private void touch() {
    this.updatedAt = Instant.now();
  }
}
