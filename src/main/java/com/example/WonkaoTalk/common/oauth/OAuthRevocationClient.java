package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
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
    authSocialRepo.findByAuthId(authId).ifPresent(social -> {
      OAuthRevocationProvider providerClient = revocationProviders.stream()
          .filter(provider -> provider.supports(social.getProvider()))
          .findFirst()
          .orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_INVALID_PROVIDER));

      // 외부 API 호출 (실패 시 예외는 퍼사드 계층에서 잡거나, 여기서 Outbox 이벤트 발행)
      // TODO: authSocial 스키마 수정 Provider RT 저장 해야함
      // providerClient.revoke(social.getProviderUserId(), social.getProviderAccessToken());
    });
  }
}
