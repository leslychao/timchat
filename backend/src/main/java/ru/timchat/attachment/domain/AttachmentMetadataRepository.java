package ru.timchat.attachment.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentMetadataRepository
    extends JpaRepository<AttachmentMetadata, UUID> {

  List<AttachmentMetadata> findByChannelId(UUID channelId);
}
