package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import com.example.WonkaoTalk.domain.auth.entity.OAuthRevocationFailure;
import com.example.WonkaoTalk.domain.auth.repo.OAuthRevocationFailureRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OAuthRevocationFailureProcessor {

  private final OAuthRevocationFailureRepo failureRepo;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveFailureEvent(AuthSocial social) {
    OAuthRevocationFailure failure = OAuthRevocationFailure.builder()
        .provider(social.getProvider())
        .providerUserId(social.getProviderUserId())
        .providerRefreshToken(social.getProviderRefreshToken())
        .build();
    failureRepo.save(failure);
  }

}
