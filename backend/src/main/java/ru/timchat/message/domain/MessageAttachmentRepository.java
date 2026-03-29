package ru.timchat.message.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageAttachmentRepository
    extends JpaRepository<MessageAttachment, UUID> {

  List<MessageAttachment> findByMessageId(UUID messageId);
}
