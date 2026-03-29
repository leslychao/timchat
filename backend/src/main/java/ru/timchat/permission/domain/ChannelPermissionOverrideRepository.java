package ru.timchat.permission.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelPermissionOverrideRepository
    extends JpaRepository<ChannelPermissionOverride, UUID> {

  List<ChannelPermissionOverride> findByChannelIdAndRoleIdIn(
      UUID channelId, List<UUID> roleIds);
}
