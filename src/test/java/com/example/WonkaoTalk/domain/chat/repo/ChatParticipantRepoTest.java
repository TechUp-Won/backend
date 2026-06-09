package com.example.WonkaoTalk.domain.chat.repo;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.WonkaoTalk.common.config.JpaConfig;
import com.example.WonkaoTalk.domain.chat.entity.ChatMessage;
import com.example.WonkaoTalk.domain.chat.entity.ChatParticipant;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.enums.MessageType;
import com.example.WonkaoTalk.domain.chat.enums.RoomType;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(properties = {
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@Import(JpaConfig.class)
class ChatParticipantRepoTest {

  @Autowired
  private ChatParticipantRepo chatParticipantRepo;

  @Autowired
  private ChatRoomRepo chatRoomRepo;

  @Autowired
  private ChatMessageRepo chatMessageRepo;

  @Test
  @DisplayName("커서가 null일 때 마지막 메시지 시간 내림차순으로 페이징 조회")
  void findMyChatRoomsByNullCursor() {
    // given
    Long myId = 1L;
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    ChatRoom room1 = createRoom(RoomType.SINGLE, now.minusDays(2));
    ChatRoom room2 = createRoom(RoomType.SINGLE, now.minusDays(1));
    ChatRoom room3 = createRoom(RoomType.SINGLE, now);

    joinRoom(room1, myId);
    joinRoom(room2, myId);
    joinRoom(room3, myId);

    // when
    Slice<ChatParticipant> result = chatParticipantRepo.findMyChatRooms(
        myId, null, null, PageRequest.of(0, 2)
    );

    // then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.getContent().get(0).getChatRoom().getId()).isEqualTo(room3.getId());
    assertThat(result.getContent().get(1).getChatRoom().getId()).isEqualTo(room2.getId());
  }

  @Test
  @DisplayName("커서가 있으면 커서보다 과거의 방부터 페이징 조회")
  void findMyChatRoomsPagingTest() {
    // given
    Long myId = 1L;
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    ChatRoom room1 = createRoom(RoomType.SINGLE, now.minusDays(3)); // 과거
    ChatRoom room2 = createRoom(RoomType.SINGLE, now.minusDays(2)); // 기준 커서
    ChatRoom room3 = createRoom(RoomType.SINGLE, now.minusDays(1)); // 최신

    joinRoom(room1, myId);
    joinRoom(room2, myId);
    joinRoom(room3, myId);

    // when
    Slice<ChatParticipant> result = chatParticipantRepo.findMyChatRooms(
        myId, room2.getLastMessageAt(), room2.getId(), PageRequest.of(0, 2)
    );

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.getContent().getFirst().getChatRoom().getId()).isEqualTo(room1.getId());
  }

  @Test
  @DisplayName("두 유저가 참여 중인 활성화된 채팅방 조회")
  void findChatRoomByUsers() {
    // given
    Long myId = 1L;
    Long receiverId = 2L;

    ChatRoom room = createRoom(RoomType.SINGLE, LocalDateTime.now());
    joinRoom(room, myId);
    joinRoom(room, receiverId);

    // 그룹 방은 조회되지 않아야 함
    ChatRoom groupRoom = createRoom(RoomType.GROUP, LocalDateTime.now());
    joinRoom(groupRoom, myId);
    joinRoom(groupRoom, receiverId);

    // when
    Optional<ChatRoom> result = chatParticipantRepo.findChatRoomByUsers(myId, receiverId);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(room.getId());
    assertThat(result.get().getRoomType()).isEqualTo(RoomType.SINGLE);
  }

  @Test
  @DisplayName("특정 방 참여자들의 마지막으로 읽은 메시지 목록 반환")
  void findAllParticipantsLastReadMessageIds() {
    // given
    ChatRoom room = createRoom(RoomType.GROUP, LocalDateTime.now());
    ChatMessage message1 = createMessage(room, "메시지1");
    ChatMessage message2 = createMessage(room, "메시지2");

    joinRoomWithBookmark(room, 1L, message1); // user1은 메시지1까지 읽음
    joinRoomWithBookmark(room, 2L, message2); // user2는 메시지2까지 읽음
    joinRoomWithBookmark(room, 3L, null); // user3은 안 읽음 (null 반환)

    // when
    List<Long> result = chatParticipantRepo.findAllParticipantsLastReadMessageIds(room.getId());

    // then
    assertThat(result).hasSize(3);
    assertThat(result).containsExactlyInAnyOrder(message1.getId(), message2.getId(), null);
  }

  // -- 헬퍼 메서드 --
  private ChatRoom createRoom(RoomType type, LocalDateTime lastAt) {
    ChatRoom room = chatRoomRepo.save(ChatRoom.builder()
        .roomType(type)
        .participantCount(2)
        .lastMessageAt(lastAt)
        .build());

    ReflectionTestUtils.setField(room, "createdAt", lastAt);
    return chatRoomRepo.save(room);
  }

  private void joinRoom(ChatRoom room, Long userId) {
    joinRoomWithBookmark(room, userId, null);
  }

  private void joinRoomWithBookmark(ChatRoom room, Long userId, ChatMessage lastReadMessage) {
    chatParticipantRepo.save(ChatParticipant.builder()
        .chatRoom(room)
        .userId(userId)
        .roomTitle("임시방제목")
        .lastReadMessage(lastReadMessage)
        .build());
  }

  private ChatMessage createMessage(ChatRoom room, String content) {
    return chatMessageRepo.save(ChatMessage.builder()
        .chatRoom(room)
        .senderId(999L)
        .content(content)
        .messageType(MessageType.TEXT)
        .build());
  }
}
