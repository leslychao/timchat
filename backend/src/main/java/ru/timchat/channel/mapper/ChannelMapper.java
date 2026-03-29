package ru.timchat.channel.mapper;

import ru.timchat.channel.api.ChannelResponse;
import ru.timchat.channel.domain.Channel;

public final class ChannelMapper {

  private ChannelMapper() {
  }

  public static ChannelResponse toResponse(Channel channel) {
    return new ChannelResponse(
        channel.getId(),
        channel.getWorkspaceId(),
        channel.getName(),
        channel.getType(),
        channel.getPosition(),
        channel.getCreatedAt(),
        channel.getUpdatedAt()
    );
  }
}
