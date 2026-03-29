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
@Table(name = "message_revisions")
public class MessageRevision {

  @Id
  private UUID id;

  @Column(name = "message_id", nullable = false)
  private UUID messageId;

  @Column(name = "previous_content", nullable = false, length = 4000)
  private String previousContent;

  @Column(name = "edited_at", nullable = false)
  private Instant editedAt;

  protected MessageRevision() {
  }

  public MessageRevision(UUID messageId, String previousContent) {
    this.id = UUID.randomUUID();
    this.messageId = messageId;
    this.previousContent = previousContent;
    this.editedAt = Instant.now();
  }
}
