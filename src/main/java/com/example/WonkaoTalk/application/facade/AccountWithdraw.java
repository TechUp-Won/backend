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
    revokeSocialConnectionSafely(authId);

    withdrawTransactionProcessor.withdrawUser(authId);

    authService.invalidateToken(email, accessToken);
  }

  public void withdrawSeller(Long authId, String email, String accessToken) {
    revokeSocialConnectionSafely(authId);

    withdrawTransactionProcessor.withdrawSeller(authId);

    authService.invalidateToken(email, accessToken);
  }

  private void revokeSocialConnectionSafely(Long authId) {
    // TODO: outbox 패턴을 통한 retry 정책 수립 필요(연동 해제 실패 시 별도의 테이블에 저장하여 AT 유효기간내에 재시도하는 scheduler 가 필요
    try {
      oAuthRevocationClient.revokeIfSocialAccountExists(authId);
    } catch (Exception e) {
      log.warn("소셜 연동 해제 실패, 내부 DB 탈퇴 로직은 계속 진행됩니다. authId: {}", authId, e);
    }
  }
}