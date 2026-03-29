package ru.timchat.permission.config;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.timchat.auth.context.CurrentUserContext;
import ru.timchat.permission.application.PermissionResolutionService;
import ru.timchat.permission.domain.PermissionType;
import ru.timchat.user.application.UserService;

/**
 * Security bean for use in @PreAuthorize expressions.
 * Usage: @PreAuthorize("@permissionSecurity.hasPermission(#workspaceId, 'MESSAGE_WRITE')")
 */
@Component("permissionSecurity")
@RequiredArgsConstructor
public class PermissionSecurity {

  private final PermissionResolutionService permissionResolutionService;
  private final UserService userService;

  public boolean hasPermission(UUID workspaceId, String permission) {
    var user = resolveCurrentUser();
    var permissionType = PermissionType.valueOf(permission);
    return permissionResolutionService.hasPermission(
        user.getId(), workspaceId, permissionType);
  }

  public boolean hasChannelPermission(
      UUID workspaceId, UUID channelId, String permission) {
    var user = resolveCurrentUser();
    var permissionType = PermissionType.valueOf(permission);
    return permissionResolutionService.hasChannelPermission(
        user.getId(), workspaceId, channelId, permissionType);
  }

  private ru.timchat.user.domain.User resolveCurrentUser() {
    return userService.findByExternalId(
        CurrentUserContext.getUserId().toString());
  }
}
