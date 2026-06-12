package com.example.WonkaoTalk.domain.chat.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageResponse;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomCreateRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomResponse;
import com.example.WonkaoTalk.domain.chat.entity.ChatMessage;
import com.example.WonkaoTalk.domain.chat.entity.ChatParticipant;
import com.example.WonkaoTalk.domain.chat.entity.ChatRoom;
import com.example.WonkaoTalk.domain.chat.enums.MessageType;
import com.example.WonkaoTalk.domain.chat.enums.RoomType;
import com.example.WonkaoTalk.domain.chat.repo.ChatMessageRepo;
import com.example.WonkaoTalk.domain.chat.repo.ChatParticipantRepo;
import com.example.WonkaoTalk.domain.chat.repo.ChatRoomRepo;
import com.example.WonkaoTalk.domain.chat.service.ChatMessageService;
import com.example.WonkaoTalk.domain.chat.service.ChatRoomService;
import com.example.WonkaoTalk.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestContainerConfig.class)
@Transactional
class ChatIntegrationTest {

  @Autowired
  private ChatRoomService chatRoomService;

  @Autowired
  private ChatMessageService chatMessageService;

  @Autowired
  private ChatRoomRepo chatRoomRepo;

  @Autowired
  private ChatParticipantRepo chatParticipantRepo;

  @Autowired
  private ChatMessageRepo chatMessageRepo;

  @PersistenceContext
  private EntityManager em;

  private User userA;
  private User userB;

  private long userSeq = 0;

  @BeforeEach
  void setUp() {
    userA = saveUser("유저A");
    userB = saveUser("유저B");
    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("채팅방 생성 성공")
  void createChatRoomSuccess() {
    // given
    ChatRoomCreateRequest request = ChatRoomCreateRequest.builder().receiverId(userB.getId())
        .build();

    // when
    ChatRoomResponse response = chatRoomService.createChatRoom(userA.getId(), request);
    em.flush();
    em.clear();

    // then
    assertThat(chatRoomRepo.count()).isEqualTo(1);
    assertThat(chatParticipantRepo.count()).isEqualTo(2);

    ChatRoom room = chatRoomRepo.findById(response.chatRoomId()).orElseThrow();
    assertThat(room.getRoomType()).isEqualTo(RoomType.SINGLE);
    assertThat(room.getParticipantCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("기존 채팅방 재사용")
  void createChatRoomWhenExistingRoom() {
    // given
    ChatRoomCreateRequest request = ChatRoomCreateRequest.builder().receiverId(userB.getId())
        .build();
    ChatRoomResponse first = chatRoomService.createChatRoom(userA.getId(), request);

    // when
    ChatRoomResponse second = chatRoomService.createChatRoom(userA.getId(), request);

    // then
    assertThat(chatRoomRepo.count()).isEqualTo(1);
    assertThat(chatParticipantRepo.count()).isEqualTo(2);
    assertThat(second.chatRoomId()).isEqualTo(first.chatRoomId());
  }

  @Test
  @DisplayName("메시지 전송 시 메시지 저장 + 마지막 메시지 갱신 + 읽음 처리")
  void sendMessageSuccess() {
    // given
    Long roomId = createRoom();
    ChatMessageRequest request = ChatMessageRequest.builder().content("안녕하세요")
        .messageType(MessageType.TEXT).build();

    // when
    ChatMessageResponse response = chatMessageService.sendMessage(userA.getId(), roomId, request);
    em.flush();
    em.clear();

    // then
    ChatRoom room = chatRoomRepo.findById(roomId).orElseThrow();
    assertThat(room.getLastMessageContent()).isEqualTo("안녕하세요");

    ChatParticipant participant = chatParticipantRepo.findByChatRoomIdAndUserId(roomId,
        userA.getId()).orElseThrow();
    assertThat(participant.getLastReadMessage()).isNotNull();
    assertThat(chatMessageRepo.count()).isEqualTo(1);
    assertThat(response.content()).isEqualTo("안녕하세요");
  }

  @Test
  @DisplayName("답장 메시지 저장")
  void sendReplyMessage() {
    // given
    Long roomId = createRoom();
    ChatRoom room = chatRoomRepo.findById(roomId).orElseThrow();
    ChatMessage original = chatMessageRepo.save(
        ChatMessage.builder().chatRoom(room).senderId(userA.getId()).content("원본")
            .messageType(MessageType.TEXT).build());

    ChatMessageRequest request = ChatMessageRequest.builder().content("답장")
        .messageType(MessageType.TEXT).answerMessageId(original.getId()).build();

    // when
    ChatMessageResponse response = chatMessageService.sendMessage(userB.getId(), roomId, request);
    em.flush();
    em.clear();

    // then
    ChatMessage reply = chatMessageRepo.findById(response.messageId()).orElseThrow();
    assertThat(reply.getAnswerMessage()).isNotNull();
    assertThat(reply.getAnswerMessage().getId()).isEqualTo(original.getId());
  }

  @Test
  @DisplayName("메시지 조회 시 마지막 읽은 메시지 갱신")
  void getMessageListUpdatesLastReadMessage() {
    // given
    Long roomId = createRoom();
    ChatRoom room = chatRoomRepo.findById(roomId).orElseThrow();
    ChatMessage latest = null;

    for (int i = 0; i < 5; i++) {
      latest = chatMessageRepo.save(
          ChatMessage.builder().chatRoom(room).senderId(userA.getId()).content("msg" + i)
              .messageType(MessageType.TEXT).build());
    }
    em.flush();
    em.clear();

    // when
    chatMessageService.getMessageList(userB.getId(), roomId, null, 20);
    em.flush();
    em.clear();

    // then
    ChatParticipant participant = chatParticipantRepo.findByChatRoomIdAndUserId(roomId,
        userB.getId()).orElseThrow();
    assertThat(participant.getLastReadMessage()).isNotNull();
    assertThat(participant.getLastReadMessage().getId()).isEqualTo(latest.getId());
  }

  private Long createRoom() {
    ChatRoomResponse response = chatRoomService.createChatRoom(userA.getId(),
        ChatRoomCreateRequest.builder().receiverId(userB.getId()).build());
    return response.chatRoomId();
  }

  private User saveUser(String nickname) {
    Auth auth = Auth.builder().build();
    em.persist(auth);
    userSeq++;
    User user = User.builder().nickname(nickname).name("테스트")
        .phone(String.format("010-%04d-%04d", userSeq, userSeq)).auth(auth).build();
    em.persist(user);
    return user;
  }
}
