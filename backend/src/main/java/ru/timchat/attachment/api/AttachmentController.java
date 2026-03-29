package ru.timchat.attachment.api;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.timchat.attachment.application.AttachmentService;
import ru.timchat.auth.context.CurrentUserContext;
import ru.timchat.user.application.UserService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AttachmentController {

  private final AttachmentService attachmentService;
  private final UserService userService;

  @PostMapping("/workspaces/{workspaceId}/attachments/upload")
  @ResponseStatus(HttpStatus.CREATED)
  public UploadUrlResponse initiateUpload(
      @PathVariable UUID workspaceId,
      @Valid @RequestBody InitUploadRequest request) {
    var user = resolveCurrentUser();
    return attachmentService.initiateUpload(
        workspaceId, user.getId(), request);
  }

  @PostMapping("/attachments/{id}/confirm")
  @ResponseStatus(HttpStatus.OK)
  public AttachmentResponse confirmUpload(@PathVariable UUID id) {
    var user = resolveCurrentUser();
    return attachmentService.confirmUpload(id, user.getId());
  }

  @GetMapping("/attachments/{id}/download-url")
  @ResponseStatus(HttpStatus.OK)
  public DownloadUrlResponse getDownloadUrl(@PathVariable UUID id) {
    var user = resolveCurrentUser();
    return attachmentService.getDownloadUrl(id, user.getId());
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
