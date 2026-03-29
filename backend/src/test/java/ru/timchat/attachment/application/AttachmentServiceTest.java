package ru.timchat.attachment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.timchat.attachment.api.InitUploadRequest;
import ru.timchat.attachment.domain.AttachmentMetadata;
import ru.timchat.attachment.domain.AttachmentMetadataRepository;
import ru.timchat.attachment.domain.AttachmentStatus;
import ru.timchat.attachment.infra.S3StorageClient;
import ru.timchat.attachment.infra.StorageProperties;
import ru.timchat.common.error.ForbiddenException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.common.error.ValidationException;
import ru.timchat.workspace.domain.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

  @Mock
  private AttachmentMetadataRepository attachmentRepository;
  @Mock
  private WorkspaceMemberRepository memberRepository;
  @Mock
  private S3StorageClient s3StorageClient;

  private StorageProperties storageProperties;
  private AttachmentService attachmentService;

  private static final UUID WORKSPACE_ID = UUID.randomUUID();
  private static final UUID CHANNEL_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID OTHER_USER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    storageProperties = new StorageProperties();
    storageProperties.setMaxFileSizeBytes(26_214_400);
    storageProperties.setAllowedContentTypes(List.of(
        "image/jpeg", "image/png", "application/pdf"));

    attachmentService = new AttachmentService(
        attachmentRepository, memberRepository,
        s3StorageClient, storageProperties);
  }

  @Test
  void initiateUpload_validRequest_returnsUploadUrl() {
    when(memberRepository.existsByWorkspaceIdAndUserId(
        WORKSPACE_ID, USER_ID)).thenReturn(true);
    when(s3StorageClient.generatePresignedUploadUrl(
        anyString(), anyString()))
        .thenReturn("https://minio/upload-url");
    when(attachmentRepository.save(any(AttachmentMetadata.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var request = new InitUploadRequest(
        CHANNEL_ID, "photo.jpg", "image/jpeg", 1024);

    var result = attachmentService
        .initiateUpload(WORKSPACE_ID, USER_ID, request);

    assertNotNull(result.attachmentId());
    assertEquals("https://minio/upload-url", result.uploadUrl());

    var captor = ArgumentCaptor
        .forClass(AttachmentMetadata.class);
    verify(attachmentRepository).save(captor.capture());
    var saved = captor.getValue();
    assertEquals(WORKSPACE_ID, saved.getWorkspaceId());
    assertEquals(CHANNEL_ID, saved.getChannelId());
    assertEquals(USER_ID, saved.getUploadedBy());
    assertEquals("photo.jpg", saved.getFileName());
    assertEquals("image/jpeg", saved.getContentType());
    assertEquals(1024, saved.getSizeBytes());
    assertEquals(AttachmentStatus.PENDING, saved.getStatus());
  }

  @Test
  void initiateUpload_invalidContentType_throwsValidation() {
    var request = new InitUploadRequest(
        CHANNEL_ID, "virus.exe", "application/x-msdownload", 1024);

    assertThrows(ValidationException.class,
        () -> attachmentService.initiateUpload(
            WORKSPACE_ID, USER_ID, request));

    verify(attachmentRepository, never()).save(any());
  }

  @Test
  void initiateUpload_exceedsMaxSize_throwsValidation() {
    var request = new InitUploadRequest(
        CHANNEL_ID, "huge.pdf", "application/pdf", 100_000_000);

    assertThrows(ValidationException.class,
        () -> attachmentService.initiateUpload(
            WORKSPACE_ID, USER_ID, request));

    verify(attachmentRepository, never()).save(any());
  }

  @Test
  void initiateUpload_zeroSize_throwsValidation() {
    var request = new InitUploadRequest(
        CHANNEL_ID, "empty.pdf", "application/pdf", 0);

    assertThrows(ValidationException.class,
        () -> attachmentService.initiateUpload(
            WORKSPACE_ID, USER_ID, request));
  }

  @Test
  void initiateUpload_notMember_throwsForbidden() {
    when(memberRepository.existsByWorkspaceIdAndUserId(
        WORKSPACE_ID, USER_ID)).thenReturn(false);

    var request = new InitUploadRequest(
        CHANNEL_ID, "photo.jpg", "image/jpeg", 1024);

    assertThrows(ForbiddenException.class,
        () -> attachmentService.initiateUpload(
            WORKSPACE_ID, USER_ID, request));
  }

  @Test
  void confirmUpload_validOwner_marksUploaded() {
    var metadata = new AttachmentMetadata(
        WORKSPACE_ID, CHANNEL_ID, USER_ID,
        "photo.jpg", "image/jpeg", 1024,
        "ws/uuid/photo.jpg");
    when(attachmentRepository.findById(metadata.getId()))
        .thenReturn(Optional.of(metadata));
    when(attachmentRepository.save(any(AttachmentMetadata.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = attachmentService
        .confirmUpload(metadata.getId(), USER_ID);

    assertEquals("UPLOADED", result.status());
    assertEquals(metadata.getId(), result.id());
  }

  @Test
  void confirmUpload_notOwner_throwsForbidden() {
    var metadata = new AttachmentMetadata(
        WORKSPACE_ID, CHANNEL_ID, USER_ID,
        "photo.jpg", "image/jpeg", 1024,
        "ws/uuid/photo.jpg");
    when(attachmentRepository.findById(metadata.getId()))
        .thenReturn(Optional.of(metadata));

    assertThrows(ForbiddenException.class,
        () -> attachmentService.confirmUpload(
            metadata.getId(), OTHER_USER_ID));
  }

  @Test
  void confirmUpload_alreadyUploaded_throwsValidation() {
    var metadata = new AttachmentMetadata(
        WORKSPACE_ID, CHANNEL_ID, USER_ID,
        "photo.jpg", "image/jpeg", 1024,
        "ws/uuid/photo.jpg");
    metadata.markUploaded();
    when(attachmentRepository.findById(metadata.getId()))
        .thenReturn(Optional.of(metadata));

    assertThrows(ValidationException.class,
        () -> attachmentService.confirmUpload(
            metadata.getId(), USER_ID));
  }

  @Test
  void confirmUpload_notFound_throwsNotFound() {
    var randomId = UUID.randomUUID();
    when(attachmentRepository.findById(randomId))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class,
        () -> attachmentService.confirmUpload(randomId, USER_ID));
  }

  @Test
  void getDownloadUrl_validUploaded_returnsUrl() {
    var metadata = new AttachmentMetadata(
        WORKSPACE_ID, CHANNEL_ID, USER_ID,
        "photo.jpg", "image/jpeg", 1024,
        "ws/uuid/photo.jpg");
    metadata.markUploaded();
    when(attachmentRepository.findById(metadata.getId()))
        .thenReturn(Optional.of(metadata));
    when(memberRepository.existsByWorkspaceIdAndUserId(
        WORKSPACE_ID, USER_ID)).thenReturn(true);
    when(s3StorageClient.generatePresignedDownloadUrl(anyString()))
        .thenReturn("https://minio/download-url");

    var result = attachmentService
        .getDownloadUrl(metadata.getId(), USER_ID);

    assertEquals("https://minio/download-url", result.downloadUrl());
    assertEquals("photo.jpg", result.fileName());
    assertEquals("image/jpeg", result.contentType());
  }

  @Test
  void getDownloadUrl_notUploaded_throwsValidation() {
    var metadata = new AttachmentMetadata(
        WORKSPACE_ID, CHANNEL_ID, USER_ID,
        "photo.jpg", "image/jpeg", 1024,
        "ws/uuid/photo.jpg");
    when(attachmentRepository.findById(metadata.getId()))
        .thenReturn(Optional.of(metadata));

    assertThrows(ValidationException.class,
        () -> attachmentService.getDownloadUrl(
            metadata.getId(), USER_ID));
  }

  @Test
  void getDownloadUrl_notMember_throwsForbidden() {
    var metadata = new AttachmentMetadata(
        WORKSPACE_ID, CHANNEL_ID, USER_ID,
        "photo.jpg", "image/jpeg", 1024,
        "ws/uuid/photo.jpg");
    metadata.markUploaded();
    when(attachmentRepository.findById(metadata.getId()))
        .thenReturn(Optional.of(metadata));
    when(memberRepository.existsByWorkspaceIdAndUserId(
        WORKSPACE_ID, OTHER_USER_ID)).thenReturn(false);

    assertThrows(ForbiddenException.class,
        () -> attachmentService.getDownloadUrl(
            metadata.getId(), OTHER_USER_ID));
  }
}
