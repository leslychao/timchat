package ru.timchat.message.application;

import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.timchat.channel.domain.Channel;
import ru.timchat.channel.domain.ChannelRepository;
import ru.timchat.common.error.ForbiddenException;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.common.error.ValidationException;
import ru.timchat.message.api.MessageResponse;
import ru.timchat.message.api.PageResponse;
import ru.timchat.message.domain.Message;
import ru.timchat.message.domain.MessageAttachment;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

  private static final int DEFAULT_LIMIT = 50;
  private static final int MAX_LIMIT = 100;

  private final MessageRepository messageRepository;
  private final MessageRevisionRepository revisionRepository;
  private final MessageDeletionRepository deletionRepository;
  private final MessageAttachmentRepository attachmentRepository;
  private final ChannelRepository channelRepository;
  private final UserRepository userRepository;
  private final PermissionResolutionService permissionResolutionService;

  @Transactional
  public MessageResponse sendMessage(UUID channelId, UUID authorId,
      String content, List<UUID> attachmentIds) {
    var channel = findChannelOrThrow(channelId);
    checkPermission(authorId, channel, PermissionType.MESSAGE_WRITE);

    var message = new Message(channelId, authorId, content);
    messageRepository.save(message);

    if (attachmentIds != null && !attachmentIds.isEmpty()) {
      for (UUID attachmentId : attachmentIds) {
        var attachment = new MessageAttachment(
            message.getId(), attachmentId);
        attachmentRepository.save(attachment);
      }
    }

    log.info("Message sent: id={}, channel={}, author={}",
        message.getId(), channelId, authorId);

    return toResponse(message);
  }

  @Transactional
  public MessageResponse editMessage(UUID messageId, UUID userId,
      String newContent) {
    var message = findMessageOrThrow(messageId);

    if (!message.getAuthorId().equals(userId)) {
      throw new ForbiddenException("error.message.edit-not-owner");
    }

    if (message.isDeleted()) {
      throw new ValidationException("error.message.already-deleted");
    }

    var revision = new MessageRevision(
        messageId, message.getContent());
    revisionRepository.save(revision);

    message.editContent(newContent);
    messageRepository.save(message);

    log.info("Message edited: id={}, userId={}", messageId, userId);

    return toResponse(message);
  }

  @Transactional
  public void deleteMessage(UUID messageId, UUID userId, String reason) {
    var message = findMessageOrThrow(messageId);

    if (message.isDeleted()) {
      return;
    }

    var channel = findChannelOrThrow(message.getChannelId());
    boolean isOwner = message.getAuthorId().equals(userId);

    if (isOwner) {
      checkPermission(
          userId, channel, PermissionType.MESSAGE_DELETE_OWN);
    } else {
      checkPermission(
          userId, channel, PermissionType.MESSAGE_DELETE_ANY);
    }

    var deletion = new MessageDeletion(messageId, userId, reason);
    deletionRepository.save(deletion);

    message.markDeleted();
    messageRepository.save(message);

    log.info("Message deleted: id={}, deletedBy={}, isOwner={}",
        messageId, userId, isOwner);
  }

  @Transactional(readOnly = true)
  public PageResponse<MessageResponse> getHistory(UUID channelId,
      String cursor, Integer limit) {
    findChannelOrThrow(channelId);

    int effectiveLimit = resolveLimit(limit);
    int fetchSize = effectiveLimit + 1;
    var pageable = PageRequest.of(0, fetchSize);

    List<Message> messages;
    if (cursor != null && !cursor.isBlank()) {
      var parsed = decodeCursor(cursor);
      messages = messageRepository.findByChannelIdBeforeCursor(
          channelId, parsed.time(), parsed.id(), pageable);
    } else {
      messages = messageRepository.findTopByChannelId(
          channelId, pageable);
    }

    boolean hasMore = messages.size() > effectiveLimit;
    var page = hasMore
        ? messages.subList(0, effectiveLimit) : messages;

    String nextCursor = null;
    if (hasMore) {
      var last = page.get(page.size() - 1);
      nextCursor = encodeCursor(last.getCreatedAt(), last.getId());
    }

    var items = page.stream()
        .map(this::toResponse)
        .toList();

    return new PageResponse<>(items, nextCursor, hasMore);
  }

  private MessageResponse toResponse(Message message) {
    String authorName = userRepository.findById(message.getAuthorId())
        .map(User::getUsername)
        .orElse("unknown");

    var attachmentIds = attachmentRepository
        .findByMessageId(message.getId())
        .stream()
        .map(MessageAttachment::getAttachmentMetadataId)
        .toList();

    return new MessageResponse(
        message.getId(),
        message.getChannelId(),
        message.getAuthorId(),
        authorName,
        message.isDeleted() ? null : message.getContent(),
        message.isDeleted(),
        message.getCreatedAt(),
        message.getUpdatedAt(),
        attachmentIds
    );
  }

  private void checkPermission(UUID userId, Channel channel,
      PermissionType permission) {
    boolean allowed = permissionResolutionService.hasChannelPermission(
        userId, channel.getWorkspaceId(),
        channel.getId(), permission);
    if (!allowed) {
      throw new ForbiddenException("error.permission.denied");
    }
  }

  private Channel findChannelOrThrow(UUID channelId) {
    return channelRepository.findById(channelId)
        .orElseThrow(() -> new NotFoundException(
            "error.channel.not-found"));
  }

  private Message findMessageOrThrow(UUID messageId) {
    return messageRepository.findById(messageId)
        .orElseThrow(() -> new NotFoundException(
            "error.message.not-found"));
  }

  private int resolveLimit(Integer limit) {
    if (limit == null || limit <= 0) {
      return DEFAULT_LIMIT;
    }
    return Math.min(limit, MAX_LIMIT);
  }

  private String encodeCursor(Instant time, UUID id) {
    var raw = time.toEpochMilli() + ":" + id.toString();
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(raw.getBytes());
  }

  private CursorValue decodeCursor(String cursor) {
    try {
      var decoded = new String(
          Base64.getUrlDecoder().decode(cursor));
      var parts = decoded.split(":", 2);
      var time = Instant.ofEpochMilli(Long.parseLong(parts[0]));
      var id = UUID.fromString(parts[1]);
      return new CursorValue(time, id);
    } catch (Exception e) {
      throw new ValidationException("error.message.invalid-cursor");
    }
  }

  private record CursorValue(Instant time, UUID id) {
  }
}
