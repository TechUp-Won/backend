package com.example.WonkaoTalk.domain.chat.repo;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.WonkaoTalk.common.config.JpaConfig;
import com.example.WonkaoTalk.domain.chat.entity.ChatParticipant;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.enums.RoomType;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaConfig.class)
class ChatRoomRepositoryTest {

  @Autowired
  private ChatRoomRepository chatRoomRepository;

  @Autowired
  private ChatParticipantRepository chatParticipantRepository;

  @Test
  @DisplayName("마지막 메시지 시간(lastMessageAt) 기준으로 내림차순 정렬되어야 한다")
  void findMyChatRoomsSortingTest() {
    // given
    Long myId = 1L;
    Long receiverId = 2L;

    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    ChatRoom room1 = createRoom("방1", //작년에 만들었는데 톡 방금 옴
        LocalDateTime.now().minusYears(1), now);

    ChatRoom room2 = createRoom("방2", //한달 전 만들었는데 톡 1시간 전에 옴
        LocalDateTime.now().minusMonths(1), now.minusHours(1));

    ChatRoom room3 = createRoom("방3", // 어제 만들었는데 톡 2시간 전에 옴
        LocalDateTime.now().minusDays(1), now.minusHours(2));

    joinRoom(room1, myId);
    joinRoom(room2, myId);
    joinRoom(room3, myId);
    joinRoom(room1, receiverId);

    // when
    Slice<ChatParticipant> result = chatParticipantRepository.findMyChatRooms(
        myId, null, null, PageRequest.of(0, 10)
    );

    // then
    List<ChatParticipant> content = result.getContent();
    assertThat(content).hasSize(3);

    assertThat(content.get(0).getChatRoom().getId()).isEqualTo(room1.getId());
    assertThat(content.get(1).getChatRoom().getId()).isEqualTo(room2.getId());
    assertThat(content.get(2).getChatRoom().getId()).isEqualTo(room3.getId());
  }

  @Test
  @DisplayName("커서 ID가 있으면 해당 ID보다 작은 방들만 조회되어야 한다")
  void findMyChatRoomsPagingTest() {
    // given
    Long myId = 1L;

    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

    ChatRoom room1 = createRoom("방1", now.minusDays(3), now.minusHours(3));
    ChatRoom room2 = createRoom("방2", now.minusDays(2), now.minusHours(2));
    ChatRoom room3 = createRoom("방3", now.minusDays(1), now.minusHours(1));

    joinRoom(room1, myId);
    joinRoom(room2, myId);
    joinRoom(room3, myId);

    // when
    Slice<ChatParticipant> result = chatParticipantRepository.findMyChatRooms(
        myId,
        room3.getLastMessageAt(),
        room3.getId(),
        PageRequest.of(0, 1)
    );

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getChatRoom().getId()).isEqualTo(room2.getId());
    assertThat(result.hasNext()).isTrue();
  }

  // -- 헬퍼 메서드 --
  private ChatRoom createRoom(String title, LocalDateTime createdAt, LocalDateTime lastAt) {
    ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
        .roomType(RoomType.SINGLE)
        .participantCount(2)
        .lastMessageAt(lastAt)
        .build());

    ReflectionTestUtils.setField(room, "createdAt", createdAt);
    return chatRoomRepository.save(room);
  }

  private void joinRoom(ChatRoom room, Long userId) {
    chatParticipantRepository.save(ChatParticipant.builder()
        .chatRoom(room)
        .userId(userId)
        .roomTitle("임시방제목")
        .build());
  }
}
