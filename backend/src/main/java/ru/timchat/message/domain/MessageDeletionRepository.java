package ru.timchat.message.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageDeletionRepository
    extends JpaRepository<MessageDeletion, UUID> {
}
