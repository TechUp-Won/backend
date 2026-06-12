//package com.example.WonkaoTalk.domain.chat.integration;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import com.example.WonkaoTalk.config.TestContainerConfig;
//import com.example.WonkaoTalk.domain.auth.entity.Auth;
//import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
//import com.example.WonkaoTalk.domain.chat.dto.ChatRoomCreateRequest;
//import com.example.WonkaoTalk.domain.chat.repo.ChatParticipantRepo;
//import com.example.WonkaoTalk.domain.chat.repo.ChatRoomRepo;
//import com.example.WonkaoTalk.domain.chat.service.ChatRoomService;
//import com.example.WonkaoTalk.domain.user.entity.User;
//import com.example.WonkaoTalk.domain.user.repo.UserRepo;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Import;
//
//@SpringBootTest
//@Import(TestContainerConfig.class)
//class ChatConcurrencyTest {
//
//  @Autowired
//  private ChatRoomService chatRoomService;
//
//  @Autowired
//  private ChatRoomRepo chatRoomRepo;
//
//  @Autowired
//  private ChatParticipantRepo chatParticipantRepo;
//
//  @Autowired
//  private UserRepo userRepo;
//
//  @Autowired
//  private AuthRepo authRepo;
//
//  private long userSeq = 0;
//
//  @Test
//  @DisplayName("동시에 채팅방 생성 시")
//  void createChatRoomConcurrently() throws Exception {
//
//    // given
//    User userA = saveUser("유저A");
//    User userB = saveUser("유저B");
//
//    Long userAId = userA.getId();
//    Long userBId = userB.getId();
//
//    ExecutorService executor = Executors.newFixedThreadPool(2);
//    CountDownLatch latch = new CountDownLatch(2);
//
//    for (int i = 0; i < 2; i++) {
//      executor.submit(() -> {
//        try {
//          chatRoomService.createChatRoom(
//              userAId,
//              ChatRoomCreateRequest.builder()
//                  .receiverId(userBId)
//                  .build()
//          );
//        } catch (Exception e) {
//          e.printStackTrace();
//        } finally {
//          latch.countDown();
//        }
//      });
//    }
//
//    latch.await();
//    executor.shutdown();
//
//    System.out.println("room count = " + chatRoomRepo.count());
//    System.out.println("participant count = " + chatParticipantRepo.count());
//
//    assertThat(chatRoomRepo.count())
//        .isEqualTo(1);
//  }
//
//  private User saveUser(String nickname) {
//    Auth auth = authRepo.save(
//        Auth.builder()
//            .build()
//    );
//
//    return userRepo.save(
//        User.builder()
//            .nickname(nickname)
//            .name("테스트")
//            .phone(String.format("010-%04d-%04d", ++userSeq, userSeq))
//            .auth(auth)
//            .build()
//    );
//  }
//}
