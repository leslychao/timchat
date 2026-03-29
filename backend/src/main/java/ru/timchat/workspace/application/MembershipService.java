package ru.timchat.workspace.application;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.timchat.common.error.ConflictException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.workspace.api.WorkspaceMemberResponse;
import ru.timchat.workspace.domain.WorkspaceMember;
import ru.timchat.workspace.domain.WorkspaceMemberRepository;
import ru.timchat.workspace.domain.WorkspaceRepository;
import ru.timchat.workspace.mapper.WorkspaceMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository memberRepository;
  private final WorkspaceMapper workspaceMapper;

  @Transactional
  public WorkspaceMemberResponse addMember(UUID workspaceId,
      UUID userId) {
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new NotFoundException("error.workspace.not-found");
    }
    if (memberRepository.existsByWorkspaceIdAndUserId(
        workspaceId, userId)) {
      throw new ConflictException("error.workspace.already-member");
    }
    var member = new WorkspaceMember(workspaceId, userId);
    memberRepository.save(member);
    log.info("Member added: workspace={}, user={}",
        workspaceId, userId);
    return workspaceMapper.toMemberResponse(member);
  }

  @Transactional
  public void removeMember(UUID workspaceId, UUID memberId) {
    var member = memberRepository.findById(memberId)
        .filter(m -> m.getWorkspaceId().equals(workspaceId))
        .orElseThrow(() -> new NotFoundException(
            "error.workspace.member-not-found"));
    memberRepository.delete(member);
    log.info("Member removed: workspace={}, memberId={}",
        workspaceId, memberId);
  }

  @Transactional(readOnly = true)
  public List<WorkspaceMemberResponse> listMembers(UUID workspaceId) {
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new NotFoundException("error.workspace.not-found");
    }
    return memberRepository.findByWorkspaceId(workspaceId).stream()
        .map(workspaceMapper::toMemberResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public boolean isMember(UUID workspaceId, UUID userId) {
    return memberRepository.existsByWorkspaceIdAndUserId(
        workspaceId, userId);
  }
}
