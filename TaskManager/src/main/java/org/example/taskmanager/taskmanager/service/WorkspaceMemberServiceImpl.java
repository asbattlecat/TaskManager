package org.example.taskmanager.taskmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.example.taskmanager.taskmanager.controller.dto.response.WorkspaceMemberDto;
import org.example.taskmanager.taskmanager.domain.entity.WorkspaceMember;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;
import org.example.taskmanager.taskmanager.infrastructure.exception.AlreadyExistsException;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.infrastructure.logs.CustomLogger;
import org.example.taskmanager.taskmanager.mapper.WorkspaceMemberMapper;
import org.example.taskmanager.taskmanager.repository.UserRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceMemberRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceMemberService;
import org.springframework.stereotype.Service;

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

  @Override
  public WorkspaceMemberDto create(UUID workspaceId, UUID userId, WorkspaceRole role) {
    log.info("creating workspace member, workspaceId={}, userId={}, role={}", workspaceId, userId, role);
    userAndWorkspaceExists(userId, workspaceId);

    // workspace member должен быть уникален для workspace
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

  @Override
  public WorkspaceMemberDto setRoleToMember(UUID memberId, WorkspaceRole role) {
    CustomLogger.operationStarts("workspaceMember", "setRoleToMember", "memberId", memberId);

    WorkspaceMember workspaceMember = getMember(memberId);

    workspaceMember.setRole(role);
    workspaceMemberRepository.save(workspaceMember);

    CustomLogger.operationCompleted("workspaceMember", "setRoleToMember", "memberId", memberId);
    return workspaceMemberMapper.toDto(workspaceMember);
  }

  @Override
  public List<WorkspaceMemberDto> getMembers(UUID workspaceId) {
    CustomLogger.operationStarts("workspaceMember", "getMembers", "workspaceId", workspaceId);

    List<WorkspaceMember> members = workspaceMemberRepository.findAllByWorkspaceId(workspaceId);

    CustomLogger.operationCompleted("workspaceMember", "getMembers", "workspaceId", workspaceId);
    return members.stream().map(workspaceMemberMapper::toDto).toList();
  }

  @Override
  public WorkspaceMemberDto delete(UUID workspaceMemberId) {
    CustomLogger.operationStarts("workspaceMember", "delete",
            "workspaceMemberId", workspaceMemberId);
    WorkspaceMember member = getMember(workspaceMemberId);

    workspaceMemberRepository.delete(member);

    CustomLogger.operationCompleted("workspaceMember", "delete",
            "workspaceMemberId", workspaceMemberId);
    return workspaceMemberMapper.toDto(member);
  }

  private void userAndWorkspaceExists(UUID userId, UUID workspaceId) {
    if (!userRepository.existsById(userId)) { // нет такого User
      log.error("user not found, userId={}", userId);
      throw new NotFoundException("User with id " + userId + " not found");
    }
    if (!workspaceRepository.existsById(workspaceId)) { // нет такого Workspace
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
