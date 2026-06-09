package com.example.WonkaoTalk.application.facade;

import com.example.WonkaoTalk.common.oauth.OAuthRevocationClient;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountWithdraw {

  private final AuthService authService;
  private final OAuthRevocationClient oAuthRevocationClient;
  private final WithdrawTransactionProcessor withdrawTransactionProcessor;

  public void withdrawUser(Long authId, String email, String accessToken) {
    withdrawTransactionProcessor.withdrawUser(authId);

    revokeSocialConnectionSafely(authId);

    authService.invalidateToken(email, accessToken);
  }

  public void withdrawSeller(Long authId, String email, String accessToken) {
    withdrawTransactionProcessor.withdrawSeller(authId);

    revokeSocialConnectionSafely(authId);

    authService.invalidateToken(email, accessToken);
  }

  private void revokeSocialConnectionSafely(Long authId) {
    try {
      oAuthRevocationClient.revokeIfSocialAccountExists(authId);
    } catch (Exception e) {
      log.warn("소셜 연동 해제 실패, 내부 DB 탈퇴 로직은 계속 진행됩니다. authId: {}", authId, e);
    }
  }
}