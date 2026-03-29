package ru.timchat.auth.config;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.timchat.auth.context.CurrentUserContext;

@RestController
@RequestMapping("/api/auth-test")
@Profile("test")
public class AuthTestController {

  @GetMapping("/me")
  @ResponseStatus(HttpStatus.OK)
  public Map<String, String> me() {
    return Map.of(
        "userId", CurrentUserContext.getUserId().toString(),
        "username", CurrentUserContext.getUsername());
  }

  @GetMapping("/roles")
  @ResponseStatus(HttpStatus.OK)
  public List<String> roles() {
    return CurrentUserContext.getRoles();
  }
}
