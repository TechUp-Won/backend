package com.example.WonkaoTalk.domain.chat.repo;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.WonkaoTalk.common.config.JpaConfig;
import com.example.WonkaoTalk.domain.chat.entity.ChatMessage;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.enums.MessageType;
import com.example.WonkaoTalk.domain.chat.enums.RoomType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

@DataJpaTest(properties = {
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(JpaConfig.class)
class ChatMessageRepoTest {

  @Autowired
  private ChatMessageRepo chatMessageRepo;

  @Autowired
  private ChatRoomRepo chatRoomRepo;

  @Test
  @DisplayName("커서가 null일 때 가장 최신 메시지부터 페이징 조회")
  void findMessagesByNullCursor() {
    // given
    ChatRoom room = createRoom();

    createMessage(room, "메시지1");
    createMessage(room, "메시지2");
    ChatMessage message3 = createMessage(room, "메시지3");

    // when
    Slice<ChatMessage> result = chatMessageRepo.findMessagesByCursor(
        room.getId(), null, PageRequest.of(0, 2)
    );

    // then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.hasNext()).isTrue(); // 3번째 메시지까지 있으니까 다음이 더 있어야 함 true
    assertThat(result.getContent().getFirst().getId()).isEqualTo(message3.getId());
    assertThat(result.getContent().getFirst().getContent()).isEqualTo("메시지3");
  }

  @Test
  @DisplayName("커서가 있을 때 커서보다 과거의 메시지부터 페이징 조회")
  void findMessagesByCursor() {
    // given
    ChatRoom room = createRoom();

    ChatMessage message1 = createMessage(room, "메시지1"); // 과거 메시지
    ChatMessage message2 = createMessage(room, "메시지2"); // 기준 커서
    createMessage(room, "메시지3");

    // when
    Slice<ChatMessage> result = chatMessageRepo.findMessagesByCursor(
        room.getId(), message2.getId(), PageRequest.of(0, 2)
    );

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.getContent().getFirst().getId()).isEqualTo(message1.getId());
    assertThat(result.getContent().getFirst().getContent()).isEqualTo("메시지1");
  }

  @Test
  @DisplayName("책갈피가 null일 때 방의 전체 메시지 개수를 반환")
  void countUnreadMessagesByNullId() {
    // given
    ChatRoom room = createRoom();

    createMessage(room, "메시지1");
    createMessage(room, "메시지2");

    // when
    int unreadCount = chatMessageRepo.countUnreadMessages(room.getId(), null);

    // then
    assertThat(unreadCount).isEqualTo(2); // 2개 모두 안 읽은 거
  }

  @Test
  @DisplayName("책갈피가 있을 때 책갈피 이후의 최신 메시지 개수만 반환")
  void countUnreadMessagesByLastReadMessageId() {
    // given
    ChatRoom room = createRoom();

    ChatMessage message1 = createMessage(room, "메시지1"); // 책갈피
    createMessage(room, "메시지2");
    createMessage(room, "메시지3");

    // when
    int unreadCount = chatMessageRepo.countUnreadMessages(room.getId(), message1.getId());

    // then
    assertThat(unreadCount).isEqualTo(2);
  }

  // -- 헬퍼 메서드 --
  private ChatRoom createRoom() {
    return chatRoomRepo.save(ChatRoom.builder()
        .roomType(RoomType.SINGLE)
        .participantCount(2)
        .build());
  }

  private ChatMessage createMessage(ChatRoom room, String content) {
    return chatMessageRepo.save(ChatMessage.builder()
        .chatRoom(room)
        .senderId(1L)
        .content(content)
        .messageType(MessageType.TEXT)
        .build());
  }
}
