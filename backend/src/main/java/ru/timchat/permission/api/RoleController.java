package ru.timchat.permission.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.timchat.permission.application.RoleService;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
@RequiredArgsConstructor
public class RoleController {

  private final RoleService roleService;

  @GetMapping("/roles")
  @ResponseStatus(HttpStatus.OK)
  public List<RoleResponse> listWorkspaceRoles(
      @PathVariable UUID workspaceId) {
    return roleService.listWorkspaceRoles(workspaceId);
  }

  @PostMapping("/members/{memberId}/roles")
  @ResponseStatus(HttpStatus.CREATED)
  public RoleResponse assignRole(
      @PathVariable UUID workspaceId,
      @PathVariable UUID memberId,
      @Valid @RequestBody AssignRoleRequest request) {
    return roleService.assignRole(
        workspaceId, memberId, request.roleName());
  }

  @DeleteMapping("/members/{memberId}/roles/{roleId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revokeRole(
      @PathVariable UUID workspaceId,
      @PathVariable UUID memberId,
      @PathVariable UUID roleId) {
    roleService.revokeRole(workspaceId, memberId, roleId);
  }

  @GetMapping("/members/{memberId}/roles")
  @ResponseStatus(HttpStatus.OK)
  public List<RoleResponse> listMemberRoles(
      @PathVariable UUID workspaceId,
      @PathVariable UUID memberId) {
    return roleService.listRolesForMember(workspaceId, memberId);
  }
}
