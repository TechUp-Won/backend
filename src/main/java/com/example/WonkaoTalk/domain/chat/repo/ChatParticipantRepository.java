package com.example.WonkaoTalk.domain.chat.repo;

import com.example.WonkaoTalk.domain.chat.entity.ChatParticipant;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import java.util.Optional;
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
}
