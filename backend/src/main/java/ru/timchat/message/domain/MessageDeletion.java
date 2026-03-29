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
@Table(name = "message_deletions")
public class MessageDeletion {

  @Id
  private UUID id;

  @Column(name = "message_id", nullable = false, unique = true)
  private UUID messageId;

  @Column(name = "deleted_by", nullable = false)
  private UUID deletedBy;

  @Column(name = "deleted_at", nullable = false)
  private Instant deletedAt;

  @Column(length = 500)
  private String reason;

  protected MessageDeletion() {
  }

  public MessageDeletion(UUID messageId, UUID deletedBy, String reason) {
    this.id = UUID.randomUUID();
    this.messageId = messageId;
    this.deletedBy = deletedBy;
    this.deletedAt = Instant.now();
    this.reason = reason;
  }
}
