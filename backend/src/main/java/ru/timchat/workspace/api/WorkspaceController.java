package ru.timchat.workspace.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.timchat.auth.context.CurrentUserContext;
import ru.timchat.user.application.UserService;
import ru.timchat.workspace.application.InviteService;
import ru.timchat.workspace.application.MembershipService;
import ru.timchat.workspace.application.WorkspaceService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkspaceController {

  private final WorkspaceService workspaceService;
  private final MembershipService membershipService;
  private final InviteService inviteService;
  private final UserService userService;

  @PostMapping("/workspaces")
  @ResponseStatus(HttpStatus.CREATED)
  public WorkspaceResponse createWorkspace(
      @Valid @RequestBody CreateWorkspaceRequest request) {
    var user = resolveCurrentUser();
    return workspaceService.create(request, user.getId());
  }

  @GetMapping("/workspaces")
  @ResponseStatus(HttpStatus.OK)
  public List<WorkspaceResponse> listWorkspaces() {
    var user = resolveCurrentUser();
    return workspaceService.listForUser(user.getId());
  }

  @GetMapping("/workspaces/{id}")
  @ResponseStatus(HttpStatus.OK)
  public WorkspaceResponse getWorkspace(@PathVariable UUID id) {
    return workspaceService.getById(id);
  }

  @PutMapping("/workspaces/{id}")
  @ResponseStatus(HttpStatus.OK)
  public WorkspaceResponse updateWorkspace(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateWorkspaceRequest request) {
    return workspaceService.update(id, request);
  }

  @DeleteMapping("/workspaces/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteWorkspace(@PathVariable UUID id) {
    workspaceService.delete(id);
  }

  @GetMapping("/workspaces/{id}/members")
  @ResponseStatus(HttpStatus.OK)
  public List<WorkspaceMemberResponse> listMembers(
      @PathVariable UUID id) {
    return membershipService.listMembers(id);
  }

  @PostMapping("/workspaces/{id}/members")
  @ResponseStatus(HttpStatus.CREATED)
  public WorkspaceMemberResponse addMember(
      @PathVariable UUID id,
      @Valid @RequestBody AddMemberRequest request) {
    return membershipService.addMember(id, request.userId());
  }

  @DeleteMapping("/workspaces/{id}/members/{memberId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeMember(
      @PathVariable UUID id,
      @PathVariable UUID memberId) {
    membershipService.removeMember(id, memberId);
  }

  @PostMapping("/workspaces/{id}/invites")
  @ResponseStatus(HttpStatus.CREATED)
  public InviteResponse createInvite(@PathVariable UUID id) {
    var user = resolveCurrentUser();
    return inviteService.createInvite(id, user.getId());
  }

  @PostMapping("/invites/{code}/accept")
  @ResponseStatus(HttpStatus.OK)
  public WorkspaceMemberResponse acceptInvite(
      @PathVariable String code) {
    var user = resolveCurrentUser();
    return inviteService.acceptInvite(code, user.getId());
  }

  private ru.timchat.user.domain.User resolveCurrentUser() {
    return userService.getOrCreateUser(
        CurrentUserContext.getUserId().toString(),
        CurrentUserContext.getUsername(),
        getEmailFromContext()
    );
  }

  private String getEmailFromContext() {
    try {
      return CurrentUserContext.getEmail();
    } catch (Exception e) {
      return CurrentUserContext.getUsername() + "@timchat.local";
    }
  }
}
