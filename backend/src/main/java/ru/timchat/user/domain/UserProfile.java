package ru.timchat.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "user_profiles")
public class UserProfile {

  @Id
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "display_name", length = 100)
  private String displayName;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Column(name = "status_text", length = 200)
  private String statusText;

  protected UserProfile() {
  }

  UserProfile(User user) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.displayName = user.getUsername();
  }

  public void updateDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void updateAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public void updateStatusText(String statusText) {
    this.statusText = statusText;
  }
}
