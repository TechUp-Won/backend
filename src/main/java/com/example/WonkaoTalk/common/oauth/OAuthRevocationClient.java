package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.domain.auth.repo.AuthSocialRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthRevocationClient {

  private final List<OAuthRevocationProvider> revocationProviders;
  private final AuthSocialRepo authSocialRepo;

  public void revokeIfSocialAccountExists(Long authId) {
    
  }
}
