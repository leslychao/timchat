package ru.timchat.workspace.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.timchat.common.error.ConflictException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.permission.application.RoleService;
import ru.timchat.workspace.api.CreateWorkspaceRequest;
import ru.timchat.workspace.api.UpdateWorkspaceRequest;
import ru.timchat.workspace.domain.Workspace;
import ru.timchat.workspace.domain.WorkspaceMember;
import ru.timchat.workspace.domain.WorkspaceMemberRepository;
import ru.timchat.workspace.domain.WorkspaceRepository;
import ru.timchat.workspace.mapper.WorkspaceMapper;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

  @Mock
  private WorkspaceRepository workspaceRepository;

  @Mock
  private WorkspaceMemberRepository memberRepository;

  @Mock
  private RoleService roleService;

  @InjectMocks
  private WorkspaceService workspaceService;

  private final WorkspaceMapper mapper = new WorkspaceMapper();

  @BeforeEach
  void setUp() {
    workspaceService = new WorkspaceService(
        workspaceRepository, memberRepository, mapper, roleService);
  }

  @Test
  void create_savesWorkspaceAndOwnerMember() {
    var request = new CreateWorkspaceRequest("My Workspace", "my-ws");
    var ownerId = UUID.randomUUID();
    when(workspaceRepository.existsBySlug("my-ws")).thenReturn(false);
    when(workspaceRepository.save(any(Workspace.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(memberRepository.save(any(WorkspaceMember.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    var response = workspaceService.create(request, ownerId);

    assertThat(response.name()).isEqualTo("My Workspace");
    assertThat(response.slug()).isEqualTo("my-ws");
    assertThat(response.ownerId()).isEqualTo(ownerId);

    var memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
    verify(memberRepository).save(memberCaptor.capture());
    assertThat(memberCaptor.getValue().getUserId()).isEqualTo(ownerId);
  }

  @Test
  void create_throwsConflictOnDuplicateSlug() {
    var request = new CreateWorkspaceRequest("Dup", "dup-slug");
    when(workspaceRepository.existsBySlug("dup-slug")).thenReturn(true);

    assertThatThrownBy(
        () -> workspaceService.create(request, UUID.randomUUID()))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void getById_throwsNotFoundForMissingWorkspace() {
    var id = UUID.randomUUID();
    when(workspaceRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> workspaceService.getById(id))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void listForUser_returnsMemberWorkspaces() {
    var userId = UUID.randomUUID();
    var ws = new Workspace("WS", "ws", userId);
    var member = new WorkspaceMember(ws.getId(), userId);
    when(memberRepository.findByUserId(userId))
        .thenReturn(List.of(member));
    when(workspaceRepository.findAllById(List.of(ws.getId())))
        .thenReturn(List.of(ws));

    var result = workspaceService.listForUser(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).slug()).isEqualTo("ws");
  }

  @Test
  void update_updatesNameAndSlug() {
    var ws = new Workspace("Old", "old-slug", UUID.randomUUID());
    when(workspaceRepository.findById(ws.getId()))
        .thenReturn(Optional.of(ws));
    when(workspaceRepository.existsBySlug("new-slug")).thenReturn(false);
    when(workspaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var request = new UpdateWorkspaceRequest("New Name", "new-slug");
    var response = workspaceService.update(ws.getId(), request);

    assertThat(response.name()).isEqualTo("New Name");
    assertThat(response.slug()).isEqualTo("new-slug");
  }

  @Test
  void delete_removesWorkspaceAndMembers() {
    var ws = new Workspace("Del", "del", UUID.randomUUID());
    var member = new WorkspaceMember(ws.getId(), UUID.randomUUID());
    when(workspaceRepository.findById(ws.getId()))
        .thenReturn(Optional.of(ws));
    when(memberRepository.findByWorkspaceId(ws.getId()))
        .thenReturn(List.of(member));

    workspaceService.delete(ws.getId());

    verify(memberRepository).delete(member);
    verify(workspaceRepository).delete(ws);
  }
}
