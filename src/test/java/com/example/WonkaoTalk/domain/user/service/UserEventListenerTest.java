package com.example.WonkaoTalk.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.event.OAuth2UserCreatedEvent;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

  @Mock
  private UserRepo userRepo;

  @InjectMocks
  private UserEventListener userEventListener;

  @Test
  @DisplayName("소셜 사용자 생성 이벤트 수신 시 User 엔티티를 저장한다")
  public void handleOAuth2UserCreatedEventSuccess() {
    // given
    Auth mockAuth = Auth.builder().build();
    OAuth2UserCreatedEvent event = new OAuth2UserCreatedEvent(mockAuth, "test@test.com", "침착",
        "010-9999-9999");

    // when
    userEventListener.handleOAuth2UserCreatedEvent(event);

    // then
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepo).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getName()).isEqualTo("침착");
    assertThat(savedUser.getNickname()).isEqualTo("침착");
    assertThat(savedUser.getPhone()).isEqualTo("010-9999-9999");
    assertThat(savedUser.getGender()).isEqualTo(Gender.NONE);
    assertThat(savedUser.isMarketingAgree()).isFalse();
  }

}