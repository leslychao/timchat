package ru.timchat.attachment.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.timchat.attachment.api.AttachmentResponse;
import ru.timchat.attachment.api.DownloadUrlResponse;
import ru.timchat.attachment.api.InitUploadRequest;
import ru.timchat.attachment.api.UploadUrlResponse;
import ru.timchat.attachment.domain.AttachmentMetadata;
import ru.timchat.attachment.domain.AttachmentMetadataRepository;
import ru.timchat.attachment.domain.AttachmentStatus;
import ru.timchat.attachment.infra.S3StorageClient;
import ru.timchat.attachment.infra.StorageProperties;
import ru.timchat.common.error.ForbiddenException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.common.error.ValidationException;
import ru.timchat.workspace.domain.WorkspaceMemberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

  private final AttachmentMetadataRepository attachmentRepository;
  private final WorkspaceMemberRepository memberRepository;
  private final S3StorageClient s3StorageClient;
  private final StorageProperties storageProperties;

  @Transactional
  public UploadUrlResponse initiateUpload(UUID workspaceId,
      UUID userId, InitUploadRequest request) {
    validateContentType(request.contentType());
    validateFileSize(request.sizeBytes());
    checkWorkspaceMembership(workspaceId, userId);

    var storageKey = buildStorageKey(
        workspaceId, request.fileName());

    var metadata = new AttachmentMetadata(
        workspaceId, request.channelId(), userId,
        request.fileName(), request.contentType(),
        request.sizeBytes(), storageKey);
    attachmentRepository.save(metadata);

    var uploadUrl = s3StorageClient
        .generatePresignedUploadUrl(storageKey, request.contentType());

    log.info("Upload initiated: id={}, workspace={}, file={}",
        metadata.getId(), workspaceId, request.fileName());

    return new UploadUrlResponse(metadata.getId(), uploadUrl);
  }

  @Transactional
  public AttachmentResponse confirmUpload(UUID attachmentId,
      UUID userId) {
    var metadata = findOrThrow(attachmentId);

    if (!metadata.getUploadedBy().equals(userId)) {
      throw new ForbiddenException(
          "error.attachment.confirm-not-owner");
    }

    if (metadata.getStatus() != AttachmentStatus.PENDING) {
      throw new ValidationException(
          "error.attachment.not-pending");
    }

    metadata.markUploaded();
    attachmentRepository.save(metadata);

    log.info("Upload confirmed: id={}", attachmentId);

    return toResponse(metadata);
  }

  @Transactional(readOnly = true)
  public DownloadUrlResponse getDownloadUrl(UUID attachmentId,
      UUID userId) {
    var metadata = findOrThrow(attachmentId);

    if (metadata.getStatus() != AttachmentStatus.UPLOADED) {
      throw new ValidationException(
          "error.attachment.not-uploaded");
    }

    checkWorkspaceMembership(metadata.getWorkspaceId(), userId);

    var downloadUrl = s3StorageClient
        .generatePresignedDownloadUrl(metadata.getStorageKey());

    return new DownloadUrlResponse(
        metadata.getId(), downloadUrl,
        metadata.getFileName(), metadata.getContentType());
  }

  private void validateContentType(String contentType) {
    var allowed = storageProperties.getAllowedContentTypes();
    if (allowed != null && !allowed.isEmpty()
        && !allowed.contains(contentType)) {
      throw new ValidationException(
          "error.attachment.invalid-content-type");
    }
  }

  private void validateFileSize(long sizeBytes) {
    if (sizeBytes <= 0) {
      throw new ValidationException(
          "error.attachment.invalid-size");
    }
    if (sizeBytes > storageProperties.getMaxFileSizeBytes()) {
      throw new ValidationException(
          "error.attachment.exceeds-size-limit");
    }
  }

  private void checkWorkspaceMembership(UUID workspaceId,
      UUID userId) {
    if (!memberRepository.existsByWorkspaceIdAndUserId(
        workspaceId, userId)) {
      throw new ForbiddenException("error.permission.denied");
    }
  }

  private AttachmentMetadata findOrThrow(UUID attachmentId) {
    return attachmentRepository.findById(attachmentId)
        .orElseThrow(() -> new NotFoundException(
            "error.attachment.not-found"));
  }

  private String buildStorageKey(UUID workspaceId, String fileName) {
    return workspaceId + "/"
        + UUID.randomUUID() + "/" + fileName;
  }

  private AttachmentResponse toResponse(AttachmentMetadata metadata) {
    return new AttachmentResponse(
        metadata.getId(),
        metadata.getWorkspaceId(),
        metadata.getChannelId(),
        metadata.getFileName(),
        metadata.getContentType(),
        metadata.getSizeBytes(),
        metadata.getStatus().name(),
        metadata.getCreatedAt());
  }
}
