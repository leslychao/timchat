package ru.timchat.permission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "roles")
public class Role {

  @Id
  private UUID id;

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "is_system", nullable = false)
  private boolean system;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Role() {
  }

  public Role(UUID workspaceId, String name, boolean system) {
    this.id = UUID.randomUUID();
    this.workspaceId = workspaceId;
    this.name = name;
    this.system = system;
    this.createdAt = Instant.now();
  }
}
