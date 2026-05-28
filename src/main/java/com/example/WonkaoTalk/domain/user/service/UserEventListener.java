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
        .phone(event.phone())
        .marketingAgree(false)
        .build();

    userRepo.save(user);
  }
}
