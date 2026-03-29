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
@Table(name = "workspaces")
public class Workspace {

  @Id
  private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, unique = true, length = 100)
  private String slug;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Workspace() {
  }

  public Workspace(String name, String slug, UUID ownerId) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.slug = slug;
    this.ownerId = ownerId;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void updateName(String name) {
    this.name = name;
    this.updatedAt = Instant.now();
  }

  public void updateSlug(String slug) {
    this.slug = slug;
    this.updatedAt = Instant.now();
  }
}
