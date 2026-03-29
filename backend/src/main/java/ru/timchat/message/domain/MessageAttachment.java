package ru.timchat.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "message_attachments")
public class MessageAttachment {

  @Id
  private UUID id;

  @Column(name = "message_id", nullable = false)
  private UUID messageId;

  @Column(name = "attachment_metadata_id")
  private UUID attachmentMetadataId;

  protected MessageAttachment() {
  }

  public MessageAttachment(UUID messageId, UUID attachmentMetadataId) {
    this.id = UUID.randomUUID();
    this.messageId = messageId;
    this.attachmentMetadataId = attachmentMetadataId;
  }
}
