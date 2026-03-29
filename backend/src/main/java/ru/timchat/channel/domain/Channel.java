package ru.timchat.channel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "channels")
public class Channel {

  @Id
  private UUID id;

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ChannelType type;

  @Column(nullable = false)
  private int position;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Channel() {
  }

  public Channel(UUID workspaceId, String name, ChannelType type,
      int position) {
    this.id = UUID.randomUUID();
    this.workspaceId = workspaceId;
    this.name = name;
    this.type = type;
    this.position = position;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void updateName(String name) {
    this.name = name;
    this.updatedAt = Instant.now();
  }

  public void updatePosition(int position) {
    this.position = position;
    this.updatedAt = Instant.now();
  }
}
