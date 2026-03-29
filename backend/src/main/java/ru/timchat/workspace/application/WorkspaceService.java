package ru.timchat.workspace.application;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.timchat.common.error.ConflictException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.permission.application.RoleService;
import ru.timchat.workspace.api.CreateWorkspaceRequest;
import ru.timchat.workspace.api.UpdateWorkspaceRequest;
import ru.timchat.workspace.api.WorkspaceResponse;
import ru.timchat.workspace.domain.Workspace;
import ru.timchat.workspace.domain.WorkspaceMember;
import ru.timchat.workspace.domain.WorkspaceMemberRepository;
import ru.timchat.workspace.domain.WorkspaceRepository;
import ru.timchat.workspace.mapper.WorkspaceMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository memberRepository;
  private final WorkspaceMapper workspaceMapper;
  private final RoleService roleService;

  @Transactional
  public WorkspaceResponse create(CreateWorkspaceRequest request,
      UUID ownerId) {
    if (workspaceRepository.existsBySlug(request.slug())) {
      throw new ConflictException("error.workspace.slug-taken");
    }
    var workspace = new Workspace(
        request.name(), request.slug(), ownerId);
    workspaceRepository.save(workspace);

    var member = new WorkspaceMember(workspace.getId(), ownerId);
    memberRepository.save(member);

    roleService.createDefaultRoles(workspace.getId());
    roleService.assignOwnerRole(workspace.getId(), member.getId());

    log.info("Workspace created: id={}, slug={}, owner={}",
        workspace.getId(), workspace.getSlug(), ownerId);
    return workspaceMapper.toResponse(workspace);
  }

  @Transactional(readOnly = true)
  public WorkspaceResponse getById(UUID workspaceId) {
    var workspace = findOrThrow(workspaceId);
    return workspaceMapper.toResponse(workspace);
  }

  @Transactional(readOnly = true)
  public List<WorkspaceResponse> listForUser(UUID userId) {
    var memberships = memberRepository.findByUserId(userId);
    var workspaceIds = memberships.stream()
        .map(WorkspaceMember::getWorkspaceId)
        .toList();
    return workspaceRepository.findAllById(workspaceIds).stream()
        .map(workspaceMapper::toResponse)
        .toList();
  }

  @Transactional
  public WorkspaceResponse update(UUID workspaceId,
      UpdateWorkspaceRequest request) {
    var workspace = findOrThrow(workspaceId);
    if (request.name() != null) {
      workspace.updateName(request.name());
    }
    if (request.slug() != null) {
      if (!workspace.getSlug().equals(request.slug())
          && workspaceRepository.existsBySlug(request.slug())) {
        throw new ConflictException("error.workspace.slug-taken");
      }
      workspace.updateSlug(request.slug());
    }
    workspaceRepository.save(workspace);
    log.info("Workspace updated: id={}", workspaceId);
    return workspaceMapper.toResponse(workspace);
  }

  @Transactional
  public void delete(UUID workspaceId) {
    var workspace = findOrThrow(workspaceId);
    memberRepository.findByWorkspaceId(workspaceId)
        .forEach(memberRepository::delete);
    workspaceRepository.delete(workspace);
    log.info("Workspace deleted: id={}", workspaceId);
  }

  private Workspace findOrThrow(UUID workspaceId) {
    return workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new NotFoundException(
            "error.workspace.not-found"));
  }
}
