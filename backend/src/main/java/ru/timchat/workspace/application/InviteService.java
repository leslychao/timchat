package ru.timchat.workspace.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.timchat.common.error.ConflictException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.common.error.ValidationException;
import ru.timchat.workspace.api.InviteResponse;
import ru.timchat.workspace.api.WorkspaceMemberResponse;
import ru.timchat.workspace.domain.Invite;
import ru.timchat.workspace.domain.InviteRepository;
import ru.timchat.workspace.domain.WorkspaceMember;
import ru.timchat.workspace.domain.WorkspaceMemberRepository;
import ru.timchat.workspace.domain.WorkspaceRepository;
import ru.timchat.workspace.mapper.WorkspaceMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class InviteService {

  private static final int DEFAULT_INVITE_EXPIRY_DAYS = 7;

  private final InviteRepository inviteRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository memberRepository;
  private final WorkspaceMapper workspaceMapper;

  @Transactional
  public InviteResponse createInvite(UUID workspaceId,
      UUID createdBy) {
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new NotFoundException("error.workspace.not-found");
    }
    var code = UUID.randomUUID().toString().substring(0, 8);
    var expiresAt = Instant.now()
        .plus(DEFAULT_INVITE_EXPIRY_DAYS, ChronoUnit.DAYS);
    var invite = new Invite(workspaceId, code, createdBy, expiresAt);
    inviteRepository.save(invite);
    log.info("Invite created: workspace={}, code={}", workspaceId,
        code);
    return workspaceMapper.toInviteResponse(invite);
  }

  @Transactional
  public WorkspaceMemberResponse acceptInvite(String code,
      UUID userId) {
    var invite = inviteRepository.findByCode(code)
        .orElseThrow(() -> new NotFoundException(
            "error.invite.not-found"));
    if (invite.isUsed()) {
      throw new ConflictException("error.invite.already-used");
    }
    if (invite.isExpired()) {
      throw new ValidationException("error.invite.expired");
    }
    if (memberRepository.existsByWorkspaceIdAndUserId(
        invite.getWorkspaceId(), userId)) {
      throw new ConflictException("error.workspace.already-member");
    }

    invite.markUsed(userId);
    inviteRepository.save(invite);

    var member = new WorkspaceMember(invite.getWorkspaceId(), userId);
    memberRepository.save(member);

    log.info("Invite accepted: code={}, user={}, workspace={}",
        code, userId, invite.getWorkspaceId());
    return workspaceMapper.toMemberResponse(member);
  }
}
