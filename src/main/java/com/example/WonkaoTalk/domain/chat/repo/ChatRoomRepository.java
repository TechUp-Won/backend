package com.example.WonkaoTalk.domain.chat.repo;

import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  @Query("""
      SELECT p.chatRoom FROM ChatParticipant p
      JOIN p.chatRoom r
      WHERE p.userId = :myId
      AND (:cursorId IS NULL OR r.id < :cursorId)
      ORDER BY r.lastMessageAt DESC, r.id DESC
      """)
  Slice<ChatRoom> findMyChatRooms(@Param("myId") Long myId,
      @Param("cursorId") Long cursorId,
      Pageable pageable);
}
