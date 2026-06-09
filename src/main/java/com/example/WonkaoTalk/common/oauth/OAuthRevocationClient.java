package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import com.example.WonkaoTalk.domain.auth.repo.AuthSocialRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthRevocationClient {

  private final List<OAuthRevocationProvider> revocationProviders;
  private final AuthSocialRepo authSocialRepo;
  private final OAuthRevocationFailureProcessor failureProcessor;

  public void revokeIfSocialAccountExists(Long authId) {
    List<AuthSocial> linkedSocials = authSocialRepo.findByAuthId(authId);
    for (AuthSocial social : linkedSocials) {
      OAuthRevocationProvider providerClient = revocationProviders.stream()
          .filter(provider -> provider.supports(social.getProvider()))
          .findFirst()
          .orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_INVALID_PROVIDER));

      try {
        // 외부 API 호출 시도
        providerClient.revoke(social.getProviderUserId(), social.getProviderRefreshToken());
      } catch (Exception e) {
        log.error("소셜 연동 해제 통신 실패. Outbox에 실패 이력을 기록합니다. AuthId: {}", authId, e);
        // Fallback: 실패 이력 저장 (이후 스케줄러가 재시도)
        failureProcessor.saveFailureEvent(social);
      }
    }
  }
}
