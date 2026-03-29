package ru.timchat.message.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.timchat.channel.domain.Channel;
import ru.timchat.channel.domain.ChannelRepository;
import ru.timchat.channel.domain.ChannelType;
import ru.timchat.common.error.ForbiddenException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.common.error.ValidationException;
import ru.timchat.message.domain.Message;
import ru.timchat.message.domain.MessageAttachmentRepository;
import ru.timchat.message.domain.MessageDeletion;
import ru.timchat.message.domain.MessageDeletionRepository;
import ru.timchat.message.domain.MessageRepository;
import ru.timchat.message.domain.MessageRevision;
import ru.timchat.message.domain.MessageRevisionRepository;
import ru.timchat.permission.application.PermissionResolutionService;
import ru.timchat.permission.domain.PermissionType;
import ru.timchat.user.domain.User;
import ru.timchat.user.domain.UserRepository;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @Mock private MessageRepository messageRepository;
  @Mock private MessageRevisionRepository revisionRepository;
  @Mock private MessageDeletionRepository deletionRepository;
  @Mock private MessageAttachmentRepository attachmentRepository;
  @Mock private ChannelRepository channelRepository;
  @Mock private UserRepository userRepository;
  @Mock private PermissionResolutionService permissionResolutionService;

  @InjectMocks
  private MessageService messageService;

  private static final UUID WORKSPACE_ID = UUID.randomUUID();
  private static final UUID CHANNEL_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID OTHER_USER_ID = UUID.randomUUID();

  private Channel testChannel;

  @BeforeEach
  void setUp() {
    testChannel = new Channel(
        WORKSPACE_ID, "general", ChannelType.TEXT, 0);
    try {
      var idField = Channel.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(testChannel, CHANNEL_ID);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void sendMessage_savesMessageAndReturnsResponse() {
    when(channelRepository.findById(CHANNEL_ID))
        .thenReturn(Optional.of(testChannel));
    when(permissionResolutionService.hasChannelPermission(
        USER_ID, WORKSPACE_ID, CHANNEL_ID,
        PermissionType.MESSAGE_WRITE))
        .thenReturn(true);
    when(messageRepository.save(any(Message.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(userRepository.findById(USER_ID))
        .thenReturn(Optional.of(
            new User("ext-1", "testuser", "test@test.com")));
    when(attachmentRepository.findByMessageId(any()))
        .thenReturn(Collections.emptyList());

    var result = messageService.sendMessage(
        CHANNEL_ID, USER_ID, "Hello world", null);

    assertThat(result).isNotNull();
    assertThat(result.content()).isEqualTo("Hello world");
    assertThat(result.channelId()).isEqualTo(CHANNEL_ID);
    assertThat(result.authorId()).isEqualTo(USER_ID);
    assertThat(result.deleted()).isFalse();
    verify(messageRepository).save(any(Message.class));
  }

  @Test
  void sendMessage_withoutPermission_throwsForbidden() {
    when(channelRepository.findById(CHANNEL_ID))
        .thenReturn(Optional.of(testChannel));
    when(permissionResolutionService.hasChannelPermission(
        USER_ID, WORKSPACE_ID, CHANNEL_ID,
        PermissionType.MESSAGE_WRITE))
        .thenReturn(false);

    assertThatThrownBy(() -> messageService.sendMessage(
        CHANNEL_ID, USER_ID, "Hello", null))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void sendMessage_channelNotFound_throwsNotFound() {
    when(channelRepository.findById(CHANNEL_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> messageService.sendMessage(
        CHANNEL_ID, USER_ID, "Hello", null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void editMessage_createsRevisionAndUpdatesContent() {
    var message = new Message(CHANNEL_ID, USER_ID, "Original");
    when(messageRepository.findById(message.getId()))
        .thenReturn(Optional.of(message));
    when(messageRepository.save(any(Message.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(userRepository.findById(USER_ID))
        .thenReturn(Optional.of(
            new User("ext-1", "testuser", "test@test.com")));
    when(attachmentRepository.findByMessageId(any()))
        .thenReturn(Collections.emptyList());

    var result = messageService.editMessage(
        message.getId(), USER_ID, "Updated");

    assertThat(result.content()).isEqualTo("Updated");

    var captor = ArgumentCaptor.forClass(MessageRevision.class);
    verify(revisionRepository).save(captor.capture());
    assertThat(captor.getValue().getPreviousContent())
        .isEqualTo("Original");
    assertThat(captor.getValue().getMessageId())
        .isEqualTo(message.getId());
  }

  @Test
  void editMessage_byNonOwner_throwsForbidden() {
    var message = new Message(CHANNEL_ID, USER_ID, "Original");
    when(messageRepository.findById(message.getId()))
        .thenReturn(Optional.of(message));

    assertThatThrownBy(() -> messageService.editMessage(
        message.getId(), OTHER_USER_ID, "Updated"))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void editMessage_deletedMessage_throwsValidation() {
    var message = new Message(CHANNEL_ID, USER_ID, "Original");
    message.markDeleted();
    when(messageRepository.findById(message.getId()))
        .thenReturn(Optional.of(message));

    assertThatThrownBy(() -> messageService.editMessage(
        message.getId(), USER_ID, "Updated"))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void editMessage_messageNotFound_throwsNotFound() {
    var messageId = UUID.randomUUID();
    when(messageRepository.findById(messageId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> messageService.editMessage(
        messageId, USER_ID, "Updated"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void deleteMessage_ownMessage_createsRecordAndMarksDeleted() {
    var message = new Message(CHANNEL_ID, USER_ID, "To delete");
    when(messageRepository.findById(message.getId()))
        .thenReturn(Optional.of(message));
    when(channelRepository.findById(CHANNEL_ID))
        .thenReturn(Optional.of(testChannel));
    when(permissionResolutionService.hasChannelPermission(
        USER_ID, WORKSPACE_ID, CHANNEL_ID,
        PermissionType.MESSAGE_DELETE_OWN))
        .thenReturn(true);

    messageService.deleteMessage(message.getId(), USER_ID, null);

    assertThat(message.isDeleted()).isTrue();
    var captor = ArgumentCaptor.forClass(MessageDeletion.class);
    verify(deletionRepository).save(captor.capture());
    assertThat(captor.getValue().getDeletedBy()).isEqualTo(USER_ID);
  }

  @Test
  void deleteMessage_otherMessage_requiresDeleteAny() {
    var message = new Message(CHANNEL_ID, OTHER_USER_ID, "Other msg");
    when(messageRepository.findById(message.getId()))
        .thenReturn(Optional.of(message));
    when(channelRepository.findById(CHANNEL_ID))
        .thenReturn(Optional.of(testChannel));
    when(permissionResolutionService.hasChannelPermission(
        USER_ID, WORKSPACE_ID, CHANNEL_ID,
        PermissionType.MESSAGE_DELETE_ANY))
        .thenReturn(false);

    assertThatThrownBy(() -> messageService.deleteMessage(
        message.getId(), USER_ID, null))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void deleteMessage_alreadyDeleted_isIdempotent() {
    var message = new Message(CHANNEL_ID, USER_ID, "Already gone");
    message.markDeleted();
    when(messageRepository.findById(message.getId()))
        .thenReturn(Optional.of(message));

    messageService.deleteMessage(message.getId(), USER_ID, null);

    verify(deletionRepository, never()).save(any());
  }

  @Test
  void getHistory_withoutCursor_returnsFirstPage() {
    var message = new Message(CHANNEL_ID, USER_ID, "Hello");
    when(channelRepository.findById(CHANNEL_ID))
        .thenReturn(Optional.of(testChannel));
    when(messageRepository.findTopByChannelId(
        eq(CHANNEL_ID), any()))
        .thenReturn(List.of(message));
    when(userRepository.findById(USER_ID))
        .thenReturn(Optional.of(
            new User("ext-1", "testuser", "test@test.com")));
    when(attachmentRepository.findByMessageId(any()))
        .thenReturn(Collections.emptyList());

    var result = messageService.getHistory(CHANNEL_ID, null, 50);

    assertThat(result.items()).hasSize(1);
    assertThat(result.hasMore()).isFalse();
    assertThat(result.nextCursor()).isNull();
  }

  @Test
  void getHistory_channelNotFound_throwsNotFound() {
    when(channelRepository.findById(CHANNEL_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> messageService.getHistory(
        CHANNEL_ID, null, 50))
        .isInstanceOf(NotFoundException.class);
  }
}
