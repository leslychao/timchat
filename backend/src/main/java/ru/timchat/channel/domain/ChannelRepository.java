package ru.timchat.channel.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

  List<Channel> findByWorkspaceIdOrderByPositionAsc(UUID workspaceId);

  int countByWorkspaceId(UUID workspaceId);
}
