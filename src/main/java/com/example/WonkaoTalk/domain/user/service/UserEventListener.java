package com.example.WonkaoTalk.domain.user.service;

import com.example.WonkaoTalk.domain.auth.event.OAuth2UserCreatedEvent;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

  private final UserRepo userRepo;

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  public void handleOAuth2UserCreatedEvent(OAuth2UserCreatedEvent event) {
    User user = User.builder()
        .auth(event.auth())
        .name(event.name())
        .nickname(event.name())
        .gender(Gender.NONE)
        //.phone() //TODO: 전화번호는 기본 설정 값으로 받아와야함!(사용자 테이블의 nullable 필드임 - 친구 추가를 위한 사용자 검색을 전화번호로 함
        .marketingAgree(false)
        .build();

    userRepo.save(user);
  }
}
