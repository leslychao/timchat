package ru.timchat.message.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

  @Query("""
      SELECT m FROM Message m
      WHERE m.channelId = :channelId AND m.deleted = false
      ORDER BY m.createdAt DESC, m.id DESC
      """)
  List<Message> findTopByChannelId(
      @Param("channelId") UUID channelId,
      org.springframework.data.domain.Pageable pageable);

  @Query("""
      SELECT m FROM Message m
      WHERE m.channelId = :channelId AND m.deleted = false
        AND (m.createdAt < :cursorTime
             OR (m.createdAt = :cursorTime AND m.id < :cursorId))
      ORDER BY m.createdAt DESC, m.id DESC
      """)
  List<Message> findByChannelIdBeforeCursor(
      @Param("channelId") UUID channelId,
      @Param("cursorTime") Instant cursorTime,
      @Param("cursorId") UUID cursorId,
      org.springframework.data.domain.Pageable pageable);
}
