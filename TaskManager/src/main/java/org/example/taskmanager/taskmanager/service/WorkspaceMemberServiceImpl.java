package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.WorkspaceMemberDto;
import org.example.taskmanager.taskmanager.domain.entity.WorkspaceMember;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceMemberState;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;
import org.example.taskmanager.taskmanager.infrastructure.exception.AlreadyExistsException;
import org.example.taskmanager.taskmanager.infrastructure.exception.EntityInUseException;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.logs.CustomLogger;
import org.example.taskmanager.taskmanager.mapper.WorkspaceMemberMapper;
import org.example.taskmanager.taskmanager.repository.UserRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceMemberRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class WorkspaceMemberServiceImpl implements WorkspaceMemberService {
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final WorkspaceMemberMapper workspaceMemberMapper;
  private final WorkspaceRepository workspaceRepository;
  private final UserRepository userRepository;

  public WorkspaceMemberServiceImpl(
          WorkspaceMemberRepository workspaceMemberRepository,
          WorkspaceMemberMapper workspaceMemberMapper,
          WorkspaceRepository workspaceRepository,
          UserRepository userRepository
  ) {
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.workspaceMemberMapper = workspaceMemberMapper;
    this.workspaceRepository = workspaceRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  @Override
  public WorkspaceMemberDto create(UUID workspaceId, UUID userId, WorkspaceRole role) {
    log.info("creating workspace member, workspaceId={}, userId={}, role={}", workspaceId, userId, role);
    userAndWorkspaceExists(userId, workspaceId);

    if (workspaceMemberRepository.existsByUserIdAndWorkspaceId(userId, workspaceId)) {
      log.error("workspace member already exists, workspaceId={}, userId={}", workspaceId, userId);
      throw new AlreadyExistsException("WorkspaceMember with workspaceId " + workspaceId
              + " and userId " + userId + " already exists");
    }

    WorkspaceMember workspaceMember = new WorkspaceMember(workspaceId, userId, role);
    workspaceMemberRepository.save(workspaceMember);

    log.debug("workspace member created and saved in database, workspaceMemberId={}", workspaceMember.getId());
    return workspaceMemberMapper.toDto(workspaceMember);
  }

  @Transactional
  @Override
  public WorkspaceMemberDto setRoleToMember(UUID memberId, WorkspaceRole role) {
    log.info("workspace member setRoleToMember operation starts, workspaceMemberId={}, role={}", memberId, role);

    WorkspaceMember workspaceMember = getMember(memberId);

    workspaceMember.setRole(role);
    workspaceMemberRepository.save(workspaceMember);
    log.debug("role to workspaceMember with id={} was set and saved in database", memberId);

    log.info("workspace member setRoleToMember operation completed, workspaceMemberId={}, role={}", memberId, role);
    return workspaceMemberMapper.toDto(workspaceMember);
  }

  @Transactional
  @Override
  public List<WorkspaceMemberDto> getMembersOfWorkspace(UUID workspaceId) {
    log.info("workspace member getMembersOfWorkspace operation starts, workspaceId={}", workspaceId);

    List<WorkspaceMember> members = workspaceMemberRepository.findAllByWorkspaceId(workspaceId);

    log.info("workspace member getMembersOfWorkspace operation completed, workspaceId={}", workspaceId);
    return members.stream().map(workspaceMemberMapper::toDto).toList();
  }

  @Transactional
  @Override
  public WorkspaceMemberDto delete(UUID workspaceMemberId) {
    log.info("workspace member delete operation starts, workspaceMemberId={}", workspaceMemberId);
    WorkspaceMember member = getMember(workspaceMemberId);

    if (member.getRole() == WorkspaceRole.OWNER) {
      log.error("workspace member not deleted, he is owner. workspaceMemberId={}", workspaceMemberId);
      throw new EntityInUseException("Cannot delete workspace member, he is owner of workspace. WorkspaceMemberId="
              + workspaceMemberId);
    }

    workspaceMemberRepository.delete(member);

    log.info("workspace member delete operation completed workspaceMemberId={}", workspaceMemberId);
    return workspaceMemberMapper.toDto(member);
  }

  @Override
  public List<WorkspaceMemberDto> archiveByUser(UUID userId) {
    log.info("workspace member archiveByUser operation starts, userId={}", userId);
    checkUserExists(userId);

    List<WorkspaceMember> members = workspaceMemberRepository.findAllByUserId(userId);
    for (WorkspaceMember member : members) {
      if (member.getState() != WorkspaceMemberState.ACTIVE) continue;

      member.archive();
      workspaceMemberRepository.save(member);
      log.debug("workspace member with id={} archived and saved in database", member.getId());
    }

    log.debug("workspace member archiveByUser operation completed, userId={}", userId);
    return members.stream().map(workspaceMemberMapper::toDto).toList();
  }

  @Override
  public List<WorkspaceMemberDto> unarchiveByUser(UUID userId) {
    log.info("workspace member unarchiveByUser operation starts, userId={}", userId);
    checkUserExists(userId);

    List<WorkspaceMember> members = workspaceMemberRepository.findAllByUserId(userId);
    for (WorkspaceMember member : members) {
      if (member.getState() != WorkspaceMemberState.USER_ARCHIVED) continue;

      member.unarchive();
      workspaceMemberRepository.save(member);
      log.debug("workspace member with id={} unarchived and saved in database", member.getId());

    }

    log.debug("workspace member unarchiveByUser operation completed, userId={}", userId);
    return members.stream().map(workspaceMemberMapper::toDto).toList();
  }

  @Override
  public WorkspaceMemberDto blockByManager(UUID workspaceMemberId) {
    log.info("workspace member blockByManager operation starts, workspaceMemberId={}", workspaceMemberId);
    WorkspaceMember member = getMember(workspaceMemberId);

    member.block();
    workspaceMemberRepository.save(member);
    log.debug("workspace member with id={} blocked and saved in database", workspaceMemberId);

    log.debug("workspace member blockByManager operation completed, workspaceMemberId={}", workspaceMemberId);
    return workspaceMemberMapper.toDto(member);
  }

  @Override
  public WorkspaceMemberDto unblockByManager(UUID workspaceMemberId) {
    log.info("workspace member unblockByManager operation starts, workspaceMemberId={}", workspaceMemberId);
    WorkspaceMember member = getMember(workspaceMemberId);

    member.unblock();
    workspaceMemberRepository.save(member);
    log.debug("workspace member with id={} unblocked and saved in database", workspaceMemberId);

    log.debug("workspace member unblockByManager operation completed, workspaceMemberId={}", workspaceMemberId);
    return workspaceMemberMapper.toDto(member);
  }

  private void userAndWorkspaceExists(UUID userId, UUID workspaceId) {
    checkUserExists(userId);
    checkWorkspaceExists(workspaceId);
  }

  private void checkUserExists(UUID userId) {
    if (!userRepository.existsById(userId)) {
      log.error("user not found, userId={}", userId);
      throw new NotFoundException("User with id " + userId + " not found");
    }
  }
  private void checkWorkspaceExists(UUID workspaceId) {
    if (!workspaceRepository.existsById(workspaceId)) {
      log.error("workspace not found, workspaceId={}", workspaceId);
      throw new NotFoundException("Workspace with id " + workspaceId + " not found");
    }
  }

  private WorkspaceMember getMember(UUID workspaceMemberId) {
    return workspaceMemberRepository.findById(workspaceMemberId)
            .orElseThrow(() -> {
              log.error("workspace member not found, workspaceMemberId={}", workspaceMemberId);
              return new NotFoundException("Member with id " + workspaceMemberId
                      + " not found");
            });
  }
}
