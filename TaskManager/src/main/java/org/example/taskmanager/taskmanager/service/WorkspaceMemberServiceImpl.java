package org.example.taskmanager.taskmanager.service;

import jakarta.validation.constraints.NotNull;
import org.example.taskmanager.taskmanager.controller.dto.*;
import org.example.taskmanager.taskmanager.domain.entity.WorkspaceMember;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.AlreadyExistsException;
import org.example.taskmanager.taskmanager.infrastructure.exceptions.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.WorkspaceMemberMapper;
import org.example.taskmanager.taskmanager.repository.UserRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceMemberRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceMemberService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class WorkspaceMemberServiceImpl implements WorkspaceMemberService {
  private final UserRepository userRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberMapper workspaceMemberMapper;

  public WorkspaceMemberServiceImpl(UserRepository userRepository,
                                    WorkspaceMemberRepository workspaceMemberRepository,
                                    WorkspaceRepository workspaceRepository,
                                    WorkspaceMemberMapper workspaceMemberMapper) {
    this.userRepository = userRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.workspaceRepository = workspaceRepository;
    this.workspaceMemberMapper = workspaceMemberMapper;
  }

  @Override
  public WorkspaceMemberDto createMember(UUID workspaceId, UUID userId, WorkspaceRole role)
          throws NotFoundException, AlreadyExistsException {
    userAndWorkspaceExists(userId, workspaceId);
    // workspace member должен быть уникален для workspace
    if (workspaceMemberRepository.
            existsByUserIdAndWorkspaceId(userId, workspaceId)) {
      throw new AlreadyExistsException("WorkspaceMember not found");
    }

    WorkspaceMember workspaceMember = new WorkspaceMember(workspaceId, userId,
            role);
    workspaceMemberRepository.save(workspaceMember);
    return workspaceMemberMapper.toDto(workspaceMember);
  }

  @Override
  public WorkspaceMemberDto deleteMember(UUID memberId, UUID workspaceId, UUID userId) throws NotFoundException {
    userAndWorkspaceExists(userId, workspaceId);
    Optional<WorkspaceMember> workspaceMemberOptional = workspaceMemberRepository.findById(memberId);
    if (workspaceMemberOptional.isEmpty()) {
      throw new NotFoundException("Such WorkspaceMember not found");
    }

    workspaceMemberRepository.delete(workspaceMemberOptional.get());
    return workspaceMemberMapper.toDto(workspaceMemberOptional.get());
  }

  @Override
  public WorkspaceMemberDto setRoleToMember(UUID workspaceMemberId, WorkspaceRole role) {
    Optional<WorkspaceMember> workspaceMemberOptional = workspaceMemberRepository.findById(workspaceMemberId);
    if (workspaceMemberOptional.isEmpty()) {
      throw new NotFoundException("WorkspaceMember not found");
    }

    WorkspaceMember workspaceMember = workspaceMemberOptional.get();
    workspaceMember.setRole(role);
    workspaceMemberRepository.save(workspaceMember);

    return workspaceMemberMapper.toDto(workspaceMember);
  }

  @Override
  public List<WorkspaceMemberDto> getMembers(UUID workspaceId) {
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new NotFoundException("Workspace not found");
    }

    List<WorkspaceMember> members = workspaceMemberRepository.findAllByWorkspaceId(workspaceId);
    return members.stream().map(workspaceMemberMapper::toDto).toList();
  }

  private void userAndWorkspaceExists(UUID userId, UUID workspaceId) {
    if (!userRepository.existsById(userId)) { // нет такого User
      throw new NotFoundException("User not found");
    }
    if (!workspaceRepository.existsById(workspaceId)) { // нет такого Workspace
      throw new NotFoundException("Workspace not found");
    }
  }
}
