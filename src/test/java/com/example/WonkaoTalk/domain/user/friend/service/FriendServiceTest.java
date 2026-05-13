package com.example.WonkaoTalk.domain.user.friend.service;

import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.friend.repo.FriendRepo;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

  @InjectMocks
  private FriendService friendService;

  @Mock
  private FriendRepo friendRepo;

  @Mock
  private UserRepo userRepo;

  private User me;
  private User target;

  @BeforeEach
  void setUp() {
    me = User.builder()
        .nickname("본인").name("나").phone("010-1111-1111")
        .build();
    ReflectionTestUtils.setField(me, "id", 1L);

    target = User.builder()
        .nickname("목표").name("너").phone("010-2222-2222")
        .build();
    ReflectionTestUtils.setField(target, "id", 2L);
  }

  @Test
  @DisplayName("친구 추가 성공")
  public void addFriendSuccess() {
    //given

    //when

    //then

  }

}