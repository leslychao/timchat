package ru.timchat.permission.application;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.timchat.common.error.ConflictException;
import ru.timchat.common.error.ForbiddenException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.permission.api.RoleResponse;
import ru.timchat.permission.domain.MemberRole;
import ru.timchat.permission.domain.MemberRoleRepository;
import ru.timchat.permission.domain.PermissionGrant;
import ru.timchat.permission.domain.PermissionGrantRepository;
import ru.timchat.permission.domain.PermissionType;
import ru.timchat.permission.domain.Role;
import ru.timchat.permission.domain.RoleRepository;
import ru.timchat.workspace.domain.WorkspaceMemberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

  private static final Map<String, Set<PermissionType>> DEFAULT_ROLE_GRANTS =
      Map.of(
          "OWNER", EnumSet.allOf(PermissionType.class),
          "ADMIN", EnumSet.of(
              PermissionType.CHANNEL_VIEW,
              PermissionType.MESSAGE_WRITE,
              PermissionType.MESSAGE_DELETE_OWN,
              PermissionType.MESSAGE_DELETE_ANY,
              PermissionType.ROOM_JOIN,
              PermissionType.ROOM_SPEAK,
              PermissionType.ROOM_VIDEO,
              PermissionType.ROOM_SCREEN_SHARE,
              PermissionType.ROOM_MODERATE,
              PermissionType.ROOM_FORCE_MUTE,
              PermissionType.ROOM_KICK),
          "MEMBER", EnumSet.of(
              PermissionType.CHANNEL_VIEW,
              PermissionType.MESSAGE_WRITE,
              PermissionType.MESSAGE_DELETE_OWN,
              PermissionType.ROOM_JOIN,
              PermissionType.ROOM_SPEAK,
              PermissionType.ROOM_VIDEO,
              PermissionType.ROOM_SCREEN_SHARE),
          "GUEST", EnumSet.of(PermissionType.CHANNEL_VIEW));

  private final RoleRepository roleRepository;
  private final PermissionGrantRepository permissionGrantRepository;
  private final MemberRoleRepository memberRoleRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  @Transactional
  public void createDefaultRoles(UUID workspaceId) {
    DEFAULT_ROLE_GRANTS.forEach((roleName, permissions) -> {
      var role = new Role(workspaceId, roleName, true);
      roleRepository.save(role);

      permissions.forEach(perm -> permissionGrantRepository.save(
          new PermissionGrant(role.getId(), perm)));
    });

    log.info("Default roles created for workspace: {}", workspaceId);
  }

  @Transactional
  public void assignOwnerRole(UUID workspaceId, UUID memberId) {
    var role = roleRepository
        .findByWorkspaceIdAndName(workspaceId, "OWNER")
        .orElseThrow(() -> new NotFoundException(
            "error.role.not-found"));
    if (!memberRoleRepository.existsByWorkspaceMemberIdAndRoleId(
        memberId, role.getId())) {
      memberRoleRepository.save(new MemberRole(memberId, role.getId()));
    }
  }

  @Transactional
  public RoleResponse assignRole(
      UUID workspaceId, UUID memberId, String roleName) {
    var member = workspaceMemberRepository.findById(memberId)
        .orElseThrow(() -> new NotFoundException(
            "error.workspace.member-not-found"));

    if (!member.getWorkspaceId().equals(workspaceId)) {
      throw new ForbiddenException("error.role.wrong-workspace");
    }

    var role = roleRepository
        .findByWorkspaceIdAndName(workspaceId, roleName)
        .orElseThrow(() -> new NotFoundException(
            "error.role.not-found"));

    if (memberRoleRepository.existsByWorkspaceMemberIdAndRoleId(
        memberId, role.getId())) {
      throw new ConflictException("error.role.already-assigned");
    }

    var memberRole = new MemberRole(memberId, role.getId());
    memberRoleRepository.save(memberRole);

    log.info("Role '{}' assigned to member {} in workspace {}",
        roleName, memberId, workspaceId);
    return toResponse(role);
  }

  @Transactional
  public void revokeRole(
      UUID workspaceId, UUID memberId, UUID roleId) {
    var role = roleRepository.findById(roleId)
        .orElseThrow(() -> new NotFoundException(
            "error.role.not-found"));

    if (!role.getWorkspaceId().equals(workspaceId)) {
      throw new ForbiddenException("error.role.wrong-workspace");
    }

    var memberRole = memberRoleRepository
        .findByWorkspaceMemberIdAndRoleId(memberId, roleId)
        .orElseThrow(() -> new NotFoundException(
            "error.role.not-assigned"));

    memberRoleRepository.delete(memberRole);
    log.info("Role '{}' revoked from member {} in workspace {}",
        role.getName(), memberId, workspaceId);
  }

  @Transactional(readOnly = true)
  public List<RoleResponse> listRolesForMember(
      UUID workspaceId, UUID memberId) {
    var member = workspaceMemberRepository.findById(memberId)
        .orElseThrow(() -> new NotFoundException(
            "error.workspace.member-not-found"));

    if (!member.getWorkspaceId().equals(workspaceId)) {
      throw new ForbiddenException("error.role.wrong-workspace");
    }

    var memberRoles = memberRoleRepository
        .findByWorkspaceMemberId(memberId);
    var roleIds = memberRoles.stream()
        .map(MemberRole::getRoleId)
        .toList();

    return roleRepository.findAllById(roleIds).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<RoleResponse> listWorkspaceRoles(UUID workspaceId) {
    return roleRepository.findByWorkspaceId(workspaceId).stream()
        .map(this::toResponse)
        .toList();
  }

  private RoleResponse toResponse(Role role) {
    return new RoleResponse(
        role.getId(),
        role.getName(),
        role.isSystem());
  }
}
