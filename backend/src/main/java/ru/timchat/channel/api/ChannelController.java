package ru.timchat.channel.api;

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
import ru.timchat.channel.application.ChannelService;
import ru.timchat.user.application.UserService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChannelController {

  private final ChannelService channelService;
  private final UserService userService;

  @PostMapping("/workspaces/{workspaceId}/channels")
  @ResponseStatus(HttpStatus.CREATED)
  public ChannelResponse createChannel(
      @PathVariable UUID workspaceId,
      @Valid @RequestBody CreateChannelRequest request) {
    return channelService.create(workspaceId, request);
  }

  @GetMapping("/workspaces/{workspaceId}/channels")
  @ResponseStatus(HttpStatus.OK)
  public List<ChannelResponse> listChannels(
      @PathVariable UUID workspaceId) {
    var user = resolveCurrentUser();
    return channelService.listVisible(workspaceId, user.getId());
  }

  @GetMapping("/channels/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ChannelResponse getChannel(@PathVariable UUID id) {
    return channelService.getById(id);
  }

  @PutMapping("/channels/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ChannelResponse updateChannel(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateChannelRequest request) {
    return channelService.update(id, request);
  }

  @DeleteMapping("/channels/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteChannel(@PathVariable UUID id) {
    channelService.delete(id);
  }

  @PutMapping("/workspaces/{workspaceId}/channels/order")
  @ResponseStatus(HttpStatus.OK)
  public List<ChannelResponse> reorderChannels(
      @PathVariable UUID workspaceId,
      @Valid @RequestBody ReorderChannelsRequest request) {
    return channelService.reorder(workspaceId, request.channelIds());
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
