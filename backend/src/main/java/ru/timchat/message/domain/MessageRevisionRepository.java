package ru.timchat.message.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRevisionRepository
    extends JpaRepository<MessageRevision, UUID> {

  List<MessageRevision> findByMessageIdOrderByEditedAtDesc(UUID messageId);
}
