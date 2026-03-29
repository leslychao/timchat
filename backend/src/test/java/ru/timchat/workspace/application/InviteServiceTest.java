package ru.timchat.workspace.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.timchat.common.error.ConflictException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.common.error.ValidationException;
import ru.timchat.workspace.domain.Invite;
import ru.timchat.workspace.domain.InviteRepository;
import ru.timchat.workspace.domain.WorkspaceMember;
import ru.timchat.workspace.domain.WorkspaceMemberRepository;
import ru.timchat.workspace.domain.WorkspaceRepository;
import ru.timchat.workspace.mapper.WorkspaceMapper;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

  @Mock
  private InviteRepository inviteRepository;

  @Mock
  private WorkspaceRepository workspaceRepository;

  @Mock
  private WorkspaceMemberRepository memberRepository;

  @InjectMocks
  private InviteService inviteService;

  private final WorkspaceMapper mapper = new WorkspaceMapper();

  @BeforeEach
  void setUp() {
    inviteService = new InviteService(
        inviteRepository, workspaceRepository, memberRepository,
        mapper);
  }

  @Test
  void createInvite_returnsInviteWithCode() {
    var workspaceId = UUID.randomUUID();
    var createdBy = UUID.randomUUID();
    when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
    when(inviteRepository.save(any(Invite.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    var response = inviteService.createInvite(workspaceId, createdBy);

    assertThat(response.code()).isNotBlank();
    assertThat(response.workspaceId()).isEqualTo(workspaceId);
  }

  @Test
  void createInvite_throwsNotFoundIfWorkspaceMissing() {
    var workspaceId = UUID.randomUUID();
    when(workspaceRepository.existsById(workspaceId)).thenReturn(false);

    assertThatThrownBy(
        () -> inviteService.createInvite(workspaceId,
            UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void acceptInvite_addsMemberAndMarksUsed() {
    var workspaceId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var invite = new Invite(workspaceId, "abc123",
        UUID.randomUUID(),
        Instant.now().plus(7, ChronoUnit.DAYS));
    when(inviteRepository.findByCode("abc123"))
        .thenReturn(Optional.of(invite));
    when(memberRepository.existsByWorkspaceIdAndUserId(
        workspaceId, userId)).thenReturn(false);
    when(inviteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(memberRepository.save(any(WorkspaceMember.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    var response = inviteService.acceptInvite("abc123", userId);

    assertThat(response.userId()).isEqualTo(userId);
    assertThat(response.workspaceId()).isEqualTo(workspaceId);
    assertThat(invite.isUsed()).isTrue();
  }

  @Test
  void acceptInvite_throwsNotFoundForMissingCode() {
    when(inviteRepository.findByCode("bad"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> inviteService.acceptInvite("bad", UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void acceptInvite_throwsConflictIfAlreadyUsed() {
    var invite = new Invite(UUID.randomUUID(), "used1",
        UUID.randomUUID(),
        Instant.now().plus(7, ChronoUnit.DAYS));
    invite.markUsed(UUID.randomUUID());
    when(inviteRepository.findByCode("used1"))
        .thenReturn(Optional.of(invite));

    assertThatThrownBy(
        () -> inviteService.acceptInvite("used1", UUID.randomUUID()))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void acceptInvite_throwsValidationIfExpired() {
    var invite = new Invite(UUID.randomUUID(), "exp1",
        UUID.randomUUID(),
        Instant.now().minus(1, ChronoUnit.DAYS));
    when(inviteRepository.findByCode("exp1"))
        .thenReturn(Optional.of(invite));

    assertThatThrownBy(
        () -> inviteService.acceptInvite("exp1", UUID.randomUUID()))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void acceptInvite_throwsConflictIfAlreadyMember() {
    var workspaceId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var invite = new Invite(workspaceId, "mem1",
        UUID.randomUUID(),
        Instant.now().plus(7, ChronoUnit.DAYS));
    when(inviteRepository.findByCode("mem1"))
        .thenReturn(Optional.of(invite));
    when(memberRepository.existsByWorkspaceIdAndUserId(
        workspaceId, userId)).thenReturn(true);

    assertThatThrownBy(
        () -> inviteService.acceptInvite("mem1", userId))
        .isInstanceOf(ConflictException.class);
  }
}
