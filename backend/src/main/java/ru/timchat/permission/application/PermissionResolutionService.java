package ru.timchat.permission.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.timchat.permission.domain.ChannelPermissionOverride;
import ru.timchat.permission.domain.ChannelPermissionOverrideRepository;
import ru.timchat.permission.domain.MemberRole;
import ru.timchat.permission.domain.MemberRoleRepository;
import ru.timchat.permission.domain.PermissionGrant;
import ru.timchat.permission.domain.PermissionGrantRepository;
import ru.timchat.permission.domain.PermissionType;
import ru.timchat.workspace.domain.WorkspaceMemberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionResolutionService {

  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final MemberRoleRepository memberRoleRepository;
  private final PermissionGrantRepository permissionGrantRepository;
  private final ChannelPermissionOverrideRepository overrideRepository;

  @Transactional(readOnly = true)
  public boolean hasPermission(
      UUID userId, UUID workspaceId, PermissionType permission) {
    var grantedPermissions = resolveWorkspacePermissions(
        userId, workspaceId);
    return grantedPermissions.contains(permission);
  }

  @Transactional(readOnly = true)
  public boolean hasChannelPermission(
      UUID userId, UUID workspaceId, UUID channelId,
      PermissionType permission) {
    var member = workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId)
        .orElse(null);
    if (member == null) {
      return false;
    }

    var memberRoles = memberRoleRepository
        .findByWorkspaceMemberId(member.getId());
    if (memberRoles.isEmpty()) {
      return false;
    }

    var roleIds = memberRoles.stream()
        .map(MemberRole::getRoleId)
        .toList();

    var baseGranted = permissionGrantRepository.findByRoleIdIn(roleIds)
        .stream()
        .map(PermissionGrant::getPermissionType)
        .collect(Collectors.toSet());

    var overrides = overrideRepository
        .findByChannelIdAndRoleIdIn(channelId, roleIds);

    return applyOverrides(baseGranted, overrides, permission);
  }

  private Set<PermissionType> resolveWorkspacePermissions(
      UUID userId, UUID workspaceId) {
    var member = workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId)
        .orElse(null);
    if (member == null) {
      return Set.of();
    }

    var memberRoles = memberRoleRepository
        .findByWorkspaceMemberId(member.getId());
    if (memberRoles.isEmpty()) {
      return Set.of();
    }

    var roleIds = memberRoles.stream()
        .map(MemberRole::getRoleId)
        .toList();

    return permissionGrantRepository.findByRoleIdIn(roleIds).stream()
        .map(PermissionGrant::getPermissionType)
        .collect(Collectors.toSet());
  }

  /**
   * Channel overrides: if any override for the permission exists,
   * it takes precedence. If multiple roles have overrides for the
   * same permission, allow wins (union semantics).
   */
  private boolean applyOverrides(
      Set<PermissionType> basePermissions,
      List<ChannelPermissionOverride> overrides,
      PermissionType permission) {
    var relevantOverrides = overrides.stream()
        .filter(o -> o.getPermissionType() == permission)
        .toList();

    if (relevantOverrides.isEmpty()) {
      return basePermissions.contains(permission);
    }

    return relevantOverrides.stream()
        .anyMatch(ChannelPermissionOverride::isAllowed);
  }
}
