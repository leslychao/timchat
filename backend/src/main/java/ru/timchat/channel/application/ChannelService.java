package ru.timchat.channel.application;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.timchat.channel.api.ChannelResponse;
import ru.timchat.channel.api.CreateChannelRequest;
import ru.timchat.channel.api.UpdateChannelRequest;
import ru.timchat.channel.domain.Channel;
import ru.timchat.channel.domain.ChannelRepository;
import ru.timchat.channel.mapper.ChannelMapper;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.common.error.ValidationException;
import ru.timchat.permission.application.PermissionResolutionService;
import ru.timchat.permission.domain.PermissionType;
import ru.timchat.workspace.domain.WorkspaceRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

  private final ChannelRepository channelRepository;
  private final WorkspaceRepository workspaceRepository;
  private final PermissionResolutionService permissionResolutionService;

  @Transactional
  public ChannelResponse create(UUID workspaceId,
      CreateChannelRequest request) {
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new NotFoundException("error.workspace.not-found");
    }

    int nextPosition = channelRepository.countByWorkspaceId(workspaceId);
    var channel = new Channel(
        workspaceId, request.name(), request.type(), nextPosition);
    channelRepository.save(channel);

    log.info("Channel created: id={}, workspace={}, name={}, type={}",
        channel.getId(), workspaceId, request.name(), request.type());

    return ChannelMapper.toResponse(channel);
  }

  @Transactional(readOnly = true)
  public ChannelResponse getById(UUID channelId) {
    var channel = findChannelOrThrow(channelId);
    return ChannelMapper.toResponse(channel);
  }

  @Transactional(readOnly = true)
  public List<ChannelResponse> listVisible(UUID workspaceId,
      UUID userId) {
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new NotFoundException("error.workspace.not-found");
    }

    var channels = channelRepository
        .findByWorkspaceIdOrderByPositionAsc(workspaceId);

    return channels.stream()
        .filter(ch -> permissionResolutionService.hasChannelPermission(
            userId, workspaceId, ch.getId(),
            PermissionType.CHANNEL_VIEW))
        .map(ChannelMapper::toResponse)
        .toList();
  }

  @Transactional
  public ChannelResponse update(UUID channelId,
      UpdateChannelRequest request) {
    var channel = findChannelOrThrow(channelId);
    channel.updateName(request.name());
    channelRepository.save(channel);

    log.info("Channel updated: id={}, newName={}",
        channelId, request.name());

    return ChannelMapper.toResponse(channel);
  }

  @Transactional
  public void delete(UUID channelId) {
    var channel = findChannelOrThrow(channelId);
    channelRepository.delete(channel);
    log.info("Channel deleted: id={}", channelId);
  }

  @Transactional
  public List<ChannelResponse> reorder(UUID workspaceId,
      List<UUID> channelIds) {
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new NotFoundException("error.workspace.not-found");
    }

    var channels = channelRepository
        .findByWorkspaceIdOrderByPositionAsc(workspaceId);
    var channelMap = channels.stream()
        .collect(Collectors.toMap(Channel::getId, ch -> ch));

    for (UUID id : channelIds) {
      if (!channelMap.containsKey(id)) {
        throw new ValidationException("error.channel.not-in-workspace");
      }
    }

    if (channelIds.size() != channels.size()) {
      throw new ValidationException(
          "error.channel.reorder-incomplete");
    }

    for (int i = 0; i < channelIds.size(); i++) {
      var channel = channelMap.get(channelIds.get(i));
      channel.updatePosition(i);
    }
    channelRepository.saveAll(channels);

    log.info("Channels reordered: workspace={}", workspaceId);

    return channelRepository
        .findByWorkspaceIdOrderByPositionAsc(workspaceId)
        .stream()
        .map(ChannelMapper::toResponse)
        .toList();
  }

  private Channel findChannelOrThrow(UUID channelId) {
    return channelRepository.findById(channelId)
        .orElseThrow(() -> new NotFoundException(
            "error.channel.not-found"));
  }
}
