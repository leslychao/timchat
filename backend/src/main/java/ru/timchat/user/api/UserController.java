package ru.timchat.user.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.timchat.auth.context.CurrentUserContext;
import ru.timchat.user.application.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  @ResponseStatus(HttpStatus.OK)
  public UserProfileResponse getCurrentUser() {
    var user = userService.getOrCreateUser(
        CurrentUserContext.getUserId().toString(),
        CurrentUserContext.getUsername(),
        getEmailFromContext()
    );
    return userService.getProfile(user.getId());
  }

  @PutMapping("/me/profile")
  @ResponseStatus(HttpStatus.OK)
  public UserProfileResponse updateProfile(
      @Valid @RequestBody UpdateProfileRequest request) {
    var user = userService.getOrCreateUser(
        CurrentUserContext.getUserId().toString(),
        CurrentUserContext.getUsername(),
        getEmailFromContext()
    );
    return userService.updateProfile(user.getId(), request);
  }

  private String getEmailFromContext() {
    try {
      return CurrentUserContext.getEmail();
    } catch (Exception e) {
      return CurrentUserContext.getUsername() + "@timchat.local";
    }
  }
}
