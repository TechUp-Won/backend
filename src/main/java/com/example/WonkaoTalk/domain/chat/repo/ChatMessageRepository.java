package com.example.WonkaoTalk.domain.chat.repo;

import com.example.WonkaoTalk.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  @Query("""
      SELECT m FROM ChatMessage m
      WHERE m.chatRoom.id = :chatRoomId
      AND (:cursorId IS NULL OR m.id < :cursorId)
      ORDER BY m.id DESC
      """)
  Slice<ChatMessage> findMessagesByCursor(
      @Param("chatRoomId") Long chatRoomId,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );

}
