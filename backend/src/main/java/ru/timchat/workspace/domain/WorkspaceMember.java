package ru.timchat.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "workspace_members")
public class WorkspaceMember {

  @Id
  private UUID id;

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "joined_at", nullable = false, updatable = false)
  private Instant joinedAt;

  protected WorkspaceMember() {
  }

  public WorkspaceMember(UUID workspaceId, UUID userId) {
    this.id = UUID.randomUUID();
    this.workspaceId = workspaceId;
    this.userId = userId;
    this.joinedAt = Instant.now();
  }
}
