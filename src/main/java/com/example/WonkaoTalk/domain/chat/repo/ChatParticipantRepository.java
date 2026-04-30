package com.example.WonkaoTalk.domain.chat.repo;

import com.example.WonkaoTalk.domain.chat.entity.ChatParticipant;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

  //이미 참여 중인 방이 있는지?
  @Query("SELECT p1.chatRoom FROM ChatParticipant p1 " +
      "JOIN ChatParticipant p2 ON p1.chatRoom.id = p2.chatRoom.id " +
      "WHERE p1.userId = :myId AND p2.userId = :receiverId " +
      "AND p1.chatRoom.roomType = 'SINGLE' " +
      "AND p1.chatRoom.roomStatus = 'ACTIVE'")
  Optional<ChatRoom> findChatRoomByUsers(@Param("myId") Long myId,
      @Param("receiverId") Long receiverId);

  // 2. 내가 참여 중인 채팅방 목록
  @Query("""
      SELECT p FROM ChatParticipant p
      JOIN FETCH p.chatRoom r
      WHERE p.userId = :myId
      AND (
          :lastMessageAt IS NULL
          OR r.lastMessageAt < :lastMessageAt
          OR (r.lastMessageAt = :lastMessageAt AND r.id < :cursorId)
      )
      ORDER BY r.lastMessageAt DESC, r.id DESC
      """)
  Slice<ChatParticipant> findMyChatRooms(
      @Param("myId") Long myId,
      @Param("lastMessageAt") LocalDateTime lastMessageAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );
}
