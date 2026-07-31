package com.example.WonkaoTalk.application.facade;

import com.example.WonkaoTalk.common.oauth.OAuthRevocationClient;
import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import com.example.WonkaoTalk.domain.auth.service.AuthCommandService;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountWithdraw {

  private final AuthService authService;
  private final AuthCommandService authCommandService;
  private final OAuthRevocationClient oAuthRevocationClient;
  private final WithdrawTransactionProcessor withdrawTransactionProcessor;

  public void withdrawUser(Long authId, String email, String accessToken) {
    List<AuthSocial> linkedSocials = authCommandService.getLinkedSocials(authId);

    withdrawTransactionProcessor.withdrawUser(authId);

    revokeSocialConnectionSafely(authId, linkedSocials);

    authService.invalidateToken(email, accessToken);
  }

  public void withdrawSeller(Long authId, String email, String accessToken) {
    List<AuthSocial> linkedSocials = authCommandService.getLinkedSocials(authId);

    withdrawTransactionProcessor.withdrawSeller(authId);

    revokeSocialConnectionSafely(authId, linkedSocials);

    authService.invalidateToken(email, accessToken);
  }

  private void revokeSocialConnectionSafely(Long authId, List<AuthSocial> linkedSocials) {
    try {
      log.info("Facade: 회원탈퇴 로직 수행. RevocationClient를 호출합니다.");
      oAuthRevocationClient.revokeIfSocialAccountExists(authId, linkedSocials);
    } catch (Exception e) {
      log.warn("소셜 연동 해제 실패, 내부 DB 탈퇴 로직은 계속 진행됩니다. authId: {}", authId, e);
    }
  }
}