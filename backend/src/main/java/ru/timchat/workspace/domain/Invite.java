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
@Table(name = "invites")
public class Invite {

  @Id
  private UUID id;

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(nullable = false, unique = true, length = 50)
  private String code;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_by")
  private UUID usedBy;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Invite() {
  }

  public Invite(UUID workspaceId, String code, UUID createdBy,
      Instant expiresAt) {
    this.id = UUID.randomUUID();
    this.workspaceId = workspaceId;
    this.code = code;
    this.createdBy = createdBy;
    this.expiresAt = expiresAt;
    this.createdAt = Instant.now();
  }

  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  public boolean isUsed() {
    return usedBy != null;
  }

  public void markUsed(UUID userId) {
    this.usedBy = userId;
    this.usedAt = Instant.now();
  }
}
