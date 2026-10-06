package org.example.taskmanager.taskmanager.service;

import org.example.taskmanager.taskmanager.controller.dto.response.WorkspaceMemberDto;
import org.example.taskmanager.taskmanager.domain.entity.WorkspaceMember;
import org.example.taskmanager.taskmanager.domain.enums.WorkspaceRole;
import org.example.taskmanager.taskmanager.infrastructure.exception.AlreadyExistsException;
import org.example.taskmanager.taskmanager.infrastructure.exception.NotFoundException;
import org.example.taskmanager.taskmanager.mapper.WorkspaceMemberMapper;
import org.example.taskmanager.taskmanager.repository.UserRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceMemberRepository;
import org.example.taskmanager.taskmanager.repository.WorkspaceRepository;
import org.example.taskmanager.taskmanager.service.interfaces.WorkspaceMemberService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
public class WorkspaceMemberServiceImpl implements WorkspaceMemberService {
  private final UserRepository userRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberMapper workspaceMemberMapper;

  public WorkspaceMemberServiceImpl(
          UserRepository userRepository,
          WorkspaceMemberRepository workspaceMemberRepository,
          WorkspaceRepository workspaceRepository,
          WorkspaceMemberMapper workspaceMemberMapper
  ) {
    this.userRepository = userRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.workspaceRepository = workspaceRepository;
    this.workspaceMemberMapper = workspaceMemberMapper;
  }

  @Override
  public WorkspaceMemberDto createMember(UUID workspaceId, UUID userId, WorkspaceRole role) {
    userAndWorkspaceExists(userId, workspaceId);

    // workspace member должен быть уникален для workspace
    if (workspaceMemberRepository.existsByUserIdAndWorkspaceId(userId, workspaceId))
      throw new AlreadyExistsException("WorkspaceMember with workspaceId " + workspaceId
              + " and userId " + userId + " already exists");


    WorkspaceMember workspaceMember = new WorkspaceMember(workspaceId, userId,
            role);
    workspaceMemberRepository.save(workspaceMember);
    return workspaceMemberMapper.toDto(workspaceMember);
  }

  @Override
  public WorkspaceMemberDto setRoleToMember(UUID memberId, WorkspaceRole role) {

    WorkspaceMember workspaceMember = workspaceMemberRepository.findById(memberId)
            .orElseThrow(() -> new NotFoundException("Member with id " + memberId + " not found"));

    workspaceMember.setRole(role);
    workspaceMemberRepository.save(workspaceMember);

    return workspaceMemberMapper.toDto(workspaceMember);
  }

  @Override
  public List<WorkspaceMemberDto> getMembers(UUID workspaceId) {
    List<WorkspaceMember> members = workspaceMemberRepository.findAllByWorkspaceId(workspaceId);
    if (members.isEmpty())
      throw new NotFoundException("Workspace members with workspaceId as parent id "
              + workspaceId + " not found");

    return members.stream().map(workspaceMemberMapper::toDto).toList();
  }

  @Override
  public WorkspaceMemberDto delete(UUID workspaceMemberId) {
    WorkspaceMember member = workspaceMemberRepository.findById(workspaceMemberId)
            .orElseThrow(() -> new NotFoundException("Member with id " + workspaceMemberId
                    + " not found"));

    workspaceMemberRepository.delete(member);
    return workspaceMemberMapper.toDto(member);
  }

  private void userAndWorkspaceExists(UUID userId, UUID workspaceId) {
    if (!userRepository.existsById(userId)) { // нет такого User
      throw new NotFoundException("User with id " + userId + " not found");
    }
    if (!workspaceRepository.existsById(workspaceId)) { // нет такого Workspace
      throw new NotFoundException("Workspace with id " + workspaceId + " not found");
    }
  }
}
