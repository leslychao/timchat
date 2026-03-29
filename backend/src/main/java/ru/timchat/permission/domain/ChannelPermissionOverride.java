package ru.timchat.permission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "channel_permission_overrides")
public class ChannelPermissionOverride {

  @Id
  private UUID id;

  @Column(name = "channel_id", nullable = false)
  private UUID channelId;

  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  @Enumerated(EnumType.STRING)
  @Column(name = "permission_type", nullable = false, length = 50)
  private PermissionType permissionType;

  @Column(name = "allowed", nullable = false)
  private boolean allowed;

  protected ChannelPermissionOverride() {
  }

  public ChannelPermissionOverride(
      UUID channelId, UUID roleId,
      PermissionType permissionType, boolean allowed) {
    this.id = UUID.randomUUID();
    this.channelId = channelId;
    this.roleId = roleId;
    this.permissionType = permissionType;
    this.allowed = allowed;
  }
}
