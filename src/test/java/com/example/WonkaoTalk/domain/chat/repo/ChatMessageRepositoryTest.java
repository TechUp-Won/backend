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

@DataJpaTest
@Import(JpaConfig.class)
class ChatMessageRepositoryTest {

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  @Autowired
  private ChatRoomRepository chatRoomRepository;

  @Test
  @DisplayName("커서 ID가 없을 때 가장 최신 메시지부터 조회되어야 한다")
  void findMessagesByCursor_NoCursor() {
    // given
    ChatRoom room = chatRoomRepository.save(ChatRoom.builder().roomType(RoomType.SINGLE).build());

    chatMessageRepository.save(createMessage(room, "메시지1"));
    chatMessageRepository.save(createMessage(room, "메시지2"));
    ChatMessage lastMessage = chatMessageRepository.save(createMessage(room, "메시지3"));

    // when
    Slice<ChatMessage> result = chatMessageRepository.findMessagesByCursor(
        room.getId(), null, PageRequest.of(0, 2)
    );

    // then
    assertThat(result.getContent()).hasSize(2); // 사이즈 2개만 꺼내온 게 맞는지
    assertThat(result.hasNext()).isTrue(); // 3번재 메시지까지 있으니까 다음이 더 있어야 함 true

    assertThat(result.getContent().getFirst().getId()).isEqualTo(lastMessage.getId());
    assertThat(result.getContent().getFirst().getContent()).isEqualTo("메시지3");
  }

  @Test
  @DisplayName("커서 ID가 있을 때 해당 ID보다 작은 메시지부터 조회되어야 한다")
  void findMessagesByCursor_WithCursor() {
    // given
    ChatRoom room = chatRoomRepository.save(ChatRoom.builder().roomType(RoomType.SINGLE).build());

    ChatMessage msg1 = chatMessageRepository.save(createMessage(room, "메시지1"));
    ChatMessage msg2 = chatMessageRepository.save(createMessage(room, "메시지2"));
    chatMessageRepository.save(createMessage(room, "메시지3"));

    // when
    Slice<ChatMessage> result = chatMessageRepository.findMessagesByCursor(
        room.getId(), msg2.getId(), PageRequest.of(0, 2)
    );

    // then
    assertThat(result.getContent()).hasSize(1); // 메시지2보다 작은 id = 과거 = 메시지3 한개뿐
    assertThat(result.hasNext()).isFalse();

    assertThat(result.getContent().getFirst().getId()).isEqualTo(msg1.getId());
    assertThat(result.getContent().getFirst().getContent()).isEqualTo("메시지1");
  }

  private ChatMessage createMessage(ChatRoom room, String content) {
    return ChatMessage.builder()
        .chatRoom(room)
        .senderId(1L)
        .content(content)
        .messageType(MessageType.TEXT)
        .build();
  }
}
