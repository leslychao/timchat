package ru.timchat.attachment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "attachment_metadata")
public class AttachmentMetadata {

  @Id
  private UUID id;

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "channel_id")
  private UUID channelId;

  @Column(name = "uploaded_by", nullable = false)
  private UUID uploadedBy;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 127)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "storage_key", nullable = false, length = 512)
  private String storageKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AttachmentStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected AttachmentMetadata() {
  }

  public AttachmentMetadata(UUID workspaceId, UUID channelId,
      UUID uploadedBy, String fileName, String contentType,
      long sizeBytes, String storageKey) {
    this.id = UUID.randomUUID();
    this.workspaceId = workspaceId;
    this.channelId = channelId;
    this.uploadedBy = uploadedBy;
    this.fileName = fileName;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.storageKey = storageKey;
    this.status = AttachmentStatus.PENDING;
    this.createdAt = Instant.now();
  }

  public void markUploaded() {
    this.status = AttachmentStatus.UPLOADED;
  }

  public void markFailed() {
    this.status = AttachmentStatus.FAILED;
  }
}
