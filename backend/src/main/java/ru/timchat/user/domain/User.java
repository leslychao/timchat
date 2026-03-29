package ru.timchat.user.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "users")
public class User {

  @Id
  private UUID id;

  @Column(name = "external_id", nullable = false, unique = true)
  private String externalId;

  @Column(nullable = false, length = 100)
  private String username;

  @Column(nullable = false)
  private String email;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
      fetch = FetchType.LAZY, orphanRemoval = true)
  private UserProfile profile;

  protected User() {
  }

  public User(String externalId, String username, String email) {
    this.id = UUID.randomUUID();
    this.externalId = externalId;
    this.username = username;
    this.email = email;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
    this.profile = new UserProfile(this);
  }

  public void updateUsername(String username) {
    this.username = username;
    this.updatedAt = Instant.now();
  }

  public void updateEmail(String email) {
    this.email = email;
    this.updatedAt = Instant.now();
  }
}
