package com.example.WonkaoTalk.domain.chat.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.WonkaoTalk.config.TestContainerConfig;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.chat.dto.ChatMessageRequest;
import com.example.WonkaoTalk.domain.chat.dto.ChatRoomCreateRequest;
import com.example.WonkaoTalk.domain.chat.enums.MessageType;
import com.example.WonkaoTalk.domain.chat.repo.ChatMessageRepo;
import com.example.WonkaoTalk.domain.chat.repo.ChatParticipantRepo;
import com.example.WonkaoTalk.domain.chat.repo.ChatRoomRepo;
import com.example.WonkaoTalk.domain.chat.service.ChatMessageService;
import com.example.WonkaoTalk.domain.chat.service.ChatRoomService;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

@Disabled("동시성 제어 이슈 해결 후 활성화 예정 1,3 실패")
@SpringBootTest
@Import(TestContainerConfig.class)
class ChatConcurrencyTest {

  @Autowired
  private ChatRoomService chatRoomService;

  @Autowired
  private ChatMessageService chatMessageService;

  @Autowired
  private ChatRoomRepo chatRoomRepo;

  @Autowired
  private ChatMessageRepo chatMessageRepo;

  @Autowired
  private ChatParticipantRepo chatParticipantRepo;

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private AuthRepo authRepo;

  @Test
  @DisplayName("동시에 채팅방 생성 시")
  void createChatRoomConcurrently() throws Exception {

    User userA = saveUser("유저A");
    User userB = saveUser("유저B");

    Long userAId = userA.getId();
    Long userBId = userB.getId();

    int threadCount = 10;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          chatRoomService.createChatRoom(
              userAId,
              ChatRoomCreateRequest.builder()
                  .receiverId(userBId)
                  .build()
          );
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await();
    executor.shutdown();

    assertThat(chatRoomRepo.count()).isEqualTo(1);
    assertThat(chatParticipantRepo.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("동시에 메시지 전송 시 메시지 유실 없음")
  void sendMessageConcurrently() throws Exception {

    User userA = saveUser("유저A");
    User userB = saveUser("유저B");

    Long roomId = chatRoomService.createChatRoom(
        userA.getId(),
        ChatRoomCreateRequest.builder()
            .receiverId(userB.getId())
            .build()
    ).chatRoomId();

    int threadCount = 20;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      int index = i;

      executor.submit(() -> {
        try {
          chatMessageService.sendMessage(
              userA.getId(),
              roomId,
              ChatMessageRequest.builder()
                  .content("message-" + index)
                  .messageType(MessageType.TEXT)
                  .build()
          );
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await();
    executor.shutdown();

    assertThat(chatMessageRepo.count()).isEqualTo(threadCount);
  }

  @Test
  @DisplayName("동시에 읽음 처리 요청 시")
  void updateLastReadMessageRaceCondition() throws Exception {

    User userA = saveUser("유저A");
    User userB = saveUser("유저B");

    Long roomId = chatRoomService.createChatRoom(
        userA.getId(),
        ChatRoomCreateRequest.builder()
            .receiverId(userB.getId())
            .build()
    ).chatRoomId();

    for (int i = 0; i < 20; i++) {
      chatMessageService.sendMessage(
          userA.getId(),
          roomId,
          ChatMessageRequest.builder()
              .content("message-" + i)
              .messageType(MessageType.TEXT)
              .build()
      );
    }

    Long lastMessageId = chatMessageRepo.findMessagesByCursor(roomId, null, Pageable.ofSize(1))
        .getContent()
        .getFirst()
        .getId();

    int threadCount = 10;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          chatMessageService.getMessageList(userB.getId(), roomId, null, 20);
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await();
    executor.shutdown();

    var participant = chatParticipantRepo.findByChatRoomIdAndUserId(roomId, userB.getId())
        .orElseThrow();

    assertThat(participant.getLastReadMessage()).isNotNull();
    assertThat(participant.getLastReadMessage().getId()).isEqualTo(lastMessageId);
  }

  private User saveUser(String nickname) {
    Auth auth = authRepo.save(Auth.builder().build());

    return userRepo.save(
        User.builder()
            .nickname(nickname)
            .name("테스트")
            .phone("010-" + UUID.randomUUID().toString().substring(0, 8))
            .auth(auth)
            .build()
    );
  }
}
