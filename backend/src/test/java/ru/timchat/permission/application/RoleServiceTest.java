package ru.timchat.permission.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.timchat.common.error.ConflictException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.permission.domain.MemberRole;
import ru.timchat.permission.domain.MemberRoleRepository;
import ru.timchat.permission.domain.PermissionGrant;
import ru.timchat.permission.domain.PermissionGrantRepository;
import ru.timchat.permission.domain.Role;
import ru.timchat.permission.domain.RoleRepository;
import ru.timchat.workspace.domain.WorkspaceMember;
import ru.timchat.workspace.domain.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private PermissionGrantRepository permissionGrantRepository;

  @Mock
  private MemberRoleRepository memberRoleRepository;

  @Mock
  private WorkspaceMemberRepository workspaceMemberRepository;

  private RoleService roleService;

  private final UUID workspaceId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    roleService = new RoleService(
        roleRepository, permissionGrantRepository,
        memberRoleRepository, workspaceMemberRepository);
  }

  @Test
  void createDefaultRoles_creates4RolesWithGrants() {
    when(roleRepository.save(any(Role.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(permissionGrantRepository.save(any(PermissionGrant.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    roleService.createDefaultRoles(workspaceId);

    var roleCaptor = ArgumentCaptor.forClass(Role.class);
    verify(roleRepository, times(4)).save(roleCaptor.capture());

    var roleNames = roleCaptor.getAllValues().stream()
        .map(Role::getName)
        .toList();
    assertThat(roleNames).containsExactlyInAnyOrder(
        "OWNER", "ADMIN", "MEMBER", "GUEST");

    roleCaptor.getAllValues().forEach(role -> {
      assertThat(role.getWorkspaceId()).isEqualTo(workspaceId);
      assertThat(role.isSystem()).isTrue();
    });
  }

  @Test
  void assignRole_createsAssignment() {
    var memberId = UUID.randomUUID();
    var member = createMember(memberId, workspaceId);
    var role = new Role(workspaceId, "MEMBER", true);

    when(workspaceMemberRepository.findById(memberId))
        .thenReturn(Optional.of(member));
    when(roleRepository.findByWorkspaceIdAndName(workspaceId, "MEMBER"))
        .thenReturn(Optional.of(role));
    when(memberRoleRepository.existsByWorkspaceMemberIdAndRoleId(
        memberId, role.getId()))
        .thenReturn(false);
    when(memberRoleRepository.save(any(MemberRole.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    var result = roleService.assignRole(
        workspaceId, memberId, "MEMBER");

    assertThat(result.name()).isEqualTo("MEMBER");
    verify(memberRoleRepository).save(any(MemberRole.class));
  }

  @Test
  void assignRole_throwsConflict_whenAlreadyAssigned() {
    var memberId = UUID.randomUUID();
    var member = createMember(memberId, workspaceId);
    var role = new Role(workspaceId, "MEMBER", true);

    when(workspaceMemberRepository.findById(memberId))
        .thenReturn(Optional.of(member));
    when(roleRepository.findByWorkspaceIdAndName(workspaceId, "MEMBER"))
        .thenReturn(Optional.of(role));
    when(memberRoleRepository.existsByWorkspaceMemberIdAndRoleId(
        memberId, role.getId()))
        .thenReturn(true);

    assertThatThrownBy(
        () -> roleService.assignRole(
            workspaceId, memberId, "MEMBER"))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void assignRole_throwsNotFound_whenMemberMissing() {
    var memberId = UUID.randomUUID();

    when(workspaceMemberRepository.findById(memberId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> roleService.assignRole(
            workspaceId, memberId, "MEMBER"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void revokeRole_removesAssignment() {
    var memberId = UUID.randomUUID();
    var role = new Role(workspaceId, "MEMBER", true);
    var memberRole = new MemberRole(memberId, role.getId());

    when(roleRepository.findById(role.getId()))
        .thenReturn(Optional.of(role));
    when(memberRoleRepository.findByWorkspaceMemberIdAndRoleId(
        memberId, role.getId()))
        .thenReturn(Optional.of(memberRole));

    roleService.revokeRole(workspaceId, memberId, role.getId());

    verify(memberRoleRepository).delete(memberRole);
  }

  @Test
  void revokeRole_throwsNotFound_whenNotAssigned() {
    var memberId = UUID.randomUUID();
    var role = new Role(workspaceId, "MEMBER", true);

    when(roleRepository.findById(role.getId()))
        .thenReturn(Optional.of(role));
    when(memberRoleRepository.findByWorkspaceMemberIdAndRoleId(
        memberId, role.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> roleService.revokeRole(
            workspaceId, memberId, role.getId()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void listRolesForMember_returnsAssignedRoles() {
    var memberId = UUID.randomUUID();
    var member = createMember(memberId, workspaceId);
    var role = new Role(workspaceId, "ADMIN", true);
    var memberRole = new MemberRole(memberId, role.getId());

    when(workspaceMemberRepository.findById(memberId))
        .thenReturn(Optional.of(member));
    when(memberRoleRepository.findByWorkspaceMemberId(memberId))
        .thenReturn(List.of(memberRole));
    when(roleRepository.findAllById(List.of(role.getId())))
        .thenReturn(List.of(role));

    var result = roleService.listRolesForMember(
        workspaceId, memberId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("ADMIN");
  }

  private WorkspaceMember createMember(UUID memberId, UUID wsId) {
    var member = new WorkspaceMember(wsId, UUID.randomUUID());
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
