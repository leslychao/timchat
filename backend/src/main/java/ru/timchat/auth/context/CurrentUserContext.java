package ru.timchat.auth.context;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUserContext {

  private CurrentUserContext() {
  }

  public static UUID getUserId() {
    return UUID.fromString(getJwt().getSubject());
  }

  public static String getUsername() {
    return getJwt().getClaimAsString("preferred_username");
  }

  public static String getEmail() {
    return getJwt().getClaimAsString("email");
  }

  @SuppressWarnings("unchecked")
  public static List<String> getRoles() {
    var jwt = getJwt();
    var realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess == null) {
      return Collections.emptyList();
    }
    var roles = (List<String>) realmAccess.get("roles");
    return roles != null ? roles : Collections.emptyList();
  }

  public static boolean hasRole(String role) {
    return getRoles().contains(role);
  }

  private static Jwt getJwt() {
    var authentication = SecurityContextHolder.getContext()
        .getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new IllegalStateException(
          "No JWT authentication found in SecurityContext");
    }
    return jwt;
  }
}
