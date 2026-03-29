package ru.timchat.permission.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.timchat.permission.domain.ChannelPermissionOverride;
import ru.timchat.permission.domain.ChannelPermissionOverrideRepository;
import ru.timchat.permission.domain.MemberRole;
import ru.timchat.permission.domain.MemberRoleRepository;
import ru.timchat.permission.domain.PermissionGrant;
import ru.timchat.permission.domain.PermissionGrantRepository;
import ru.timchat.permission.domain.PermissionType;
import ru.timchat.workspace.domain.WorkspaceMember;
import ru.timchat.workspace.domain.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class PermissionResolutionServiceTest {

  @Mock
  private WorkspaceMemberRepository workspaceMemberRepository;

  @Mock
  private MemberRoleRepository memberRoleRepository;

  @Mock
  private PermissionGrantRepository permissionGrantRepository;

  @Mock
  private ChannelPermissionOverrideRepository overrideRepository;

  private PermissionResolutionService service;

  private final UUID userId = UUID.randomUUID();
  private final UUID workspaceId = UUID.randomUUID();
  private final UUID channelId = UUID.randomUUID();
  private UUID memberId;
  private UUID roleId;

  @BeforeEach
  void setUp() {
    service = new PermissionResolutionService(
        workspaceMemberRepository, memberRoleRepository,
        permissionGrantRepository, overrideRepository);

    memberId = UUID.randomUUID();
    roleId = UUID.randomUUID();
  }

  @Test
  void hasPermission_returnsTrue_whenUserHasGrantedPermission() {
    var member = mockMember();
    var memberRole = new MemberRole(memberId, roleId);

    when(workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId))
        .thenReturn(Optional.of(member));
    when(memberRoleRepository.findByWorkspaceMemberId(memberId))
        .thenReturn(List.of(memberRole));
    when(permissionGrantRepository.findByRoleIdIn(List.of(roleId)))
        .thenReturn(List.of(
            new PermissionGrant(roleId, PermissionType.CHANNEL_VIEW),
            new PermissionGrant(roleId, PermissionType.MESSAGE_WRITE)));

    assertThat(service.hasPermission(
        userId, workspaceId, PermissionType.MESSAGE_WRITE)).isTrue();
  }

  @Test
  void hasPermission_returnsFalse_whenUserLacksPermission() {
    var member = mockMember();
    var memberRole = new MemberRole(memberId, roleId);

    when(workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId))
        .thenReturn(Optional.of(member));
    when(memberRoleRepository.findByWorkspaceMemberId(memberId))
        .thenReturn(List.of(memberRole));
    when(permissionGrantRepository.findByRoleIdIn(List.of(roleId)))
        .thenReturn(List.of(
            new PermissionGrant(roleId, PermissionType.CHANNEL_VIEW)));

    assertThat(service.hasPermission(
        userId, workspaceId, PermissionType.MESSAGE_WRITE)).isFalse();
  }

  @Test
  void hasPermission_returnsFalse_whenUserNotMember() {
    when(workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId))
        .thenReturn(Optional.empty());

    assertThat(service.hasPermission(
        userId, workspaceId, PermissionType.CHANNEL_VIEW)).isFalse();
  }

  @Test
  void hasPermission_returnsFalse_whenUserHasNoRoles() {
    var member = mockMember();

    when(workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId))
        .thenReturn(Optional.of(member));
    when(memberRoleRepository.findByWorkspaceMemberId(memberId))
        .thenReturn(List.of());

    assertThat(service.hasPermission(
        userId, workspaceId, PermissionType.CHANNEL_VIEW)).isFalse();
  }

  @Test
  void hasPermission_checksAllPermissionTypes() {
    var member = mockMember();
    var memberRole = new MemberRole(memberId, roleId);

    when(workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId))
        .thenReturn(Optional.of(member));
    when(memberRoleRepository.findByWorkspaceMemberId(memberId))
        .thenReturn(List.of(memberRole));

    var allGrants = List.of(
        new PermissionGrant(roleId, PermissionType.CHANNEL_VIEW),
        new PermissionGrant(roleId, PermissionType.MESSAGE_WRITE),
        new PermissionGrant(roleId, PermissionType.MESSAGE_DELETE_OWN),
        new PermissionGrant(roleId, PermissionType.MESSAGE_DELETE_ANY),
        new PermissionGrant(roleId, PermissionType.ROOM_JOIN),
        new PermissionGrant(roleId, PermissionType.ROOM_SPEAK),
        new PermissionGrant(roleId, PermissionType.ROOM_VIDEO),
        new PermissionGrant(roleId, PermissionType.ROOM_SCREEN_SHARE),
        new PermissionGrant(roleId, PermissionType.ROOM_MODERATE),
        new PermissionGrant(roleId, PermissionType.ROOM_FORCE_MUTE),
        new PermissionGrant(roleId, PermissionType.ROOM_KICK),
        new PermissionGrant(roleId, PermissionType.ROOM_MOVE));

    when(permissionGrantRepository.findByRoleIdIn(List.of(roleId)))
        .thenReturn(allGrants);

    for (PermissionType perm : PermissionType.values()) {
      assertThat(service.hasPermission(userId, workspaceId, perm))
          .as("Permission %s should be granted", perm)
          .isTrue();
    }
  }

  @Test
  void hasPermission_mergesMultipleRoles() {
    var member = mockMember();
    var roleId2 = UUID.randomUUID();
    var mr1 = new MemberRole(memberId, roleId);
    var mr2 = new MemberRole(memberId, roleId2);

    when(workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId))
        .thenReturn(Optional.of(member));
    when(memberRoleRepository.findByWorkspaceMemberId(memberId))
        .thenReturn(List.of(mr1, mr2));
    when(permissionGrantRepository
        .findByRoleIdIn(List.of(roleId, roleId2)))
        .thenReturn(List.of(
            new PermissionGrant(roleId, PermissionType.CHANNEL_VIEW),
            new PermissionGrant(roleId2, PermissionType.MESSAGE_WRITE)));

    assertThat(service.hasPermission(
        userId, workspaceId, PermissionType.CHANNEL_VIEW)).isTrue();
    assertThat(service.hasPermission(
        userId, workspaceId, PermissionType.MESSAGE_WRITE)).isTrue();
  }

  @Test
  void hasChannelPermission_usesBaseGrant_whenNoOverride() {
    var member = mockMember();
    var memberRole = new MemberRole(memberId, roleId);

    when(workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId))
        .thenReturn(Optional.of(member));
    when(memberRoleRepository.findByWorkspaceMemberId(memberId))
        .thenReturn(List.of(memberRole));
    when(permissionGrantRepository.findByRoleIdIn(List.of(roleId)))
        .thenReturn(List.of(
            new PermissionGrant(roleId, PermissionType.MESSAGE_WRITE)));
    when(overrideRepository
        .findByChannelIdAndRoleIdIn(channelId, List.of(roleId)))
        .thenReturn(List.of());

    assertThat(service.hasChannelPermission(
        userId, workspaceId, channelId,
        PermissionType.MESSAGE_WRITE)).isTrue();
  }

  @Test
  void hasChannelPermission_overrideDeny_blocksPermission() {
    var member = mockMember();
    var memberRole = new MemberRole(memberId, roleId);

    when(workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId))
        .thenReturn(Optional.of(member));
    when(memberRoleRepository.findByWorkspaceMemberId(memberId))
        .thenReturn(List.of(memberRole));
    when(permissionGrantRepository.findByRoleIdIn(List.of(roleId)))
        .thenReturn(List.of(
            new PermissionGrant(roleId, PermissionType.MESSAGE_WRITE)));

    var denyOverride = new ChannelPermissionOverride(
        channelId, roleId, PermissionType.MESSAGE_WRITE, false);
    when(overrideRepository
        .findByChannelIdAndRoleIdIn(channelId, List.of(roleId)))
        .thenReturn(List.of(denyOverride));

    assertThat(service.hasChannelPermission(
        userId, workspaceId, channelId,
        PermissionType.MESSAGE_WRITE)).isFalse();
  }

  @Test
  void hasChannelPermission_overrideAllow_grantsPermission() {
    var member = mockMember();
    var memberRole = new MemberRole(memberId, roleId);

    when(workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId))
        .thenReturn(Optional.of(member));
    when(memberRoleRepository.findByWorkspaceMemberId(memberId))
        .thenReturn(List.of(memberRole));
    when(permissionGrantRepository.findByRoleIdIn(List.of(roleId)))
        .thenReturn(List.of());

    var allowOverride = new ChannelPermissionOverride(
        channelId, roleId, PermissionType.MESSAGE_WRITE, true);
    when(overrideRepository
        .findByChannelIdAndRoleIdIn(channelId, List.of(roleId)))
        .thenReturn(List.of(allowOverride));

    assertThat(service.hasChannelPermission(
        userId, workspaceId, channelId,
        PermissionType.MESSAGE_WRITE)).isTrue();
  }

  @Test
  void hasChannelPermission_returnsFalse_whenNotMember() {
    when(workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId))
        .thenReturn(Optional.empty());

    assertThat(service.hasChannelPermission(
        userId, workspaceId, channelId,
        PermissionType.CHANNEL_VIEW)).isFalse();
  }

  @Test
  void hasChannelPermission_allowOverrideWins_whenMultipleRoles() {
    var member = mockMember();
    var roleId2 = UUID.randomUUID();
    var mr1 = new MemberRole(memberId, roleId);
    var mr2 = new MemberRole(memberId, roleId2);

    when(workspaceMemberRepository
        .findByWorkspaceIdAndUserId(workspaceId, userId))
        .thenReturn(Optional.of(member));
    when(memberRoleRepository.findByWorkspaceMemberId(memberId))
        .thenReturn(List.of(mr1, mr2));
    when(permissionGrantRepository
        .findByRoleIdIn(List.of(roleId, roleId2)))
        .thenReturn(List.of());

    var denyOverride = new ChannelPermissionOverride(
        channelId, roleId, PermissionType.MESSAGE_WRITE, false);
    var allowOverride = new ChannelPermissionOverride(
        channelId, roleId2, PermissionType.MESSAGE_WRITE, true);
    when(overrideRepository
        .findByChannelIdAndRoleIdIn(channelId, List.of(roleId, roleId2)))
        .thenReturn(List.of(denyOverride, allowOverride));

    assertThat(service.hasChannelPermission(
        userId, workspaceId, channelId,
        PermissionType.MESSAGE_WRITE)).isTrue();
  }

  private WorkspaceMember mockMember() {
    var member = new WorkspaceMember(workspaceId, userId);
    try {
      var idField = WorkspaceMember.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(member, memberId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return member;
  }
}
