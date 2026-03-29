package ru.timchat.workspace.mapper;

import org.springframework.stereotype.Component;
import ru.timchat.workspace.api.InviteResponse;
import ru.timchat.workspace.api.WorkspaceMemberResponse;
import ru.timchat.workspace.api.WorkspaceResponse;
import ru.timchat.workspace.domain.Invite;
import ru.timchat.workspace.domain.Workspace;
import ru.timchat.workspace.domain.WorkspaceMember;

@Component
public class WorkspaceMapper {

  public WorkspaceResponse toResponse(Workspace workspace) {
    return new WorkspaceResponse(
        workspace.getId(),
        workspace.getName(),
        workspace.getSlug(),
        workspace.getOwnerId(),
        workspace.getCreatedAt(),
        workspace.getUpdatedAt()
    );
  }

  public WorkspaceMemberResponse toMemberResponse(
      WorkspaceMember member) {
    return new WorkspaceMemberResponse(
        member.getId(),
        member.getWorkspaceId(),
        member.getUserId(),
        member.getJoinedAt()
    );
  }

  public InviteResponse toInviteResponse(Invite invite) {
    return new InviteResponse(
        invite.getId(),
        invite.getWorkspaceId(),
        invite.getCode(),
        invite.getExpiresAt(),
        invite.getCreatedAt()
    );
  }
}
