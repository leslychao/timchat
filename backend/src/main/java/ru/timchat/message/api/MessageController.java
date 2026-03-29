package ru.timchat.message.api;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.timchat.auth.context.CurrentUserContext;
import ru.timchat.message.application.MessageService;
import ru.timchat.user.application.UserService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

  private final MessageService messageService;
  private final UserService userService;

  @PostMapping("/channels/{channelId}/messages")
  @ResponseStatus(HttpStatus.CREATED)
  public MessageResponse sendMessage(
      @PathVariable UUID channelId,
      @Valid @RequestBody SendMessageRequest request) {
    var user = resolveCurrentUser();
    return messageService.sendMessage(
        channelId, user.getId(),
        request.content(), request.attachmentIds());
  }

  @PutMapping("/messages/{id}")
  @ResponseStatus(HttpStatus.OK)
  public MessageResponse editMessage(
      @PathVariable UUID id,
      @Valid @RequestBody EditMessageRequest request) {
    var user = resolveCurrentUser();
    return messageService.editMessage(
        id, user.getId(), request.content());
  }

  @DeleteMapping("/messages/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteMessage(@PathVariable UUID id) {
    var user = resolveCurrentUser();
    messageService.deleteMessage(id, user.getId(), null);
  }

  @GetMapping("/channels/{channelId}/messages")
  @ResponseStatus(HttpStatus.OK)
  public PageResponse<MessageResponse> getHistory(
      @PathVariable UUID channelId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer limit) {
    return messageService.getHistory(channelId, cursor, limit);
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
