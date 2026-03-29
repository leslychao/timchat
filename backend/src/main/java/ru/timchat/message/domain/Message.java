package ru.timchat.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "messages")
public class Message {

  @Id
  private UUID id;

  @Column(name = "channel_id", nullable = false)
  private UUID channelId;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column(nullable = false, length = 4000)
  private String content;

  @Column(nullable = false)
  private boolean deleted;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Message() {
  }

  public Message(UUID channelId, UUID authorId, String content) {
    this.id = UUID.randomUUID();
    this.channelId = channelId;
    this.authorId = authorId;
    this.content = content;
    this.deleted = false;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void editContent(String newContent) {
    this.content = newContent;
    this.updatedAt = Instant.now();
  }

  public void markDeleted() {
    this.deleted = true;
    this.updatedAt = Instant.now();
  }
}
