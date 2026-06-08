package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.domain.auth.entity.OAuthRevocationFailure;
import com.example.WonkaoTalk.domain.auth.enums.FallbackStatus;
import com.example.WonkaoTalk.domain.auth.repo.OAuthRevocationFailureRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthRevocationScheduler {

  private static final int MAX_RETRY_COUNT = 5;
  private final OAuthRevocationFailureRepo failureRepo;
  private final List<OAuthRevocationProvider> revocationProviders;

  // TODO: 외부 API 배치 병렬 처리로 성능 향상
  @Scheduled(cron = "0 0/10 * * * *")
  @Transactional
  public void processRevocationFailures() {
    List<OAuthRevocationFailure> pendingList =
        failureRepo.findTop50ByStatusOrderByCreatedAtAsc(FallbackStatus.PENDING);

    if (pendingList.isEmpty()) {
      return;
    }
    log.info("소셜 연동 해지 Fallback 배치를 시작합니다. 대상 건수: {}", pendingList.size());

    for (OAuthRevocationFailure failure : pendingList) {
      OAuthRevocationProvider providerClient = revocationProviders.stream()
          .filter(provider -> provider.supports(failure.getProvider()))
          .findFirst()
          .orElse(null);

      if (providerClient == null) {
        log.error("지원하지 않는 Provider입니다. 강제 영구 실패 처리합니다. ID: {}", failure.getId());
        failure.incrementRetryCount(0); // 즉시 영구 실패 처리
        continue;
      }

      try {
        // 외부 API 재시도
        providerClient.revoke(failure.getProviderUserId(), failure.getProviderRefreshToken());
        failure.markAsSuccess();
        log.info("소셜 연동 해지 재시도 성공. Failure ID: {}", failure.getId());

      } catch (Exception e) {
        failure.incrementRetryCount(MAX_RETRY_COUNT);
        log.warn("소셜 연동 해지 재시도 실패. Failure ID: {}, 현재 시도 횟수: {}",
            failure.getId(), failure.getRetryCount(), e);

        if (failure.getStatus() == FallbackStatus.PERMANENT_FAILURE) {
          log.error("소셜 연동 해지 최대 재시도 횟수({}) 초과. 영구 실패(Dead Letter) 처리됩니다. 대상: {}",
              MAX_RETRY_COUNT, failure.getProviderUserId());
          // 추가적인 알림 로직을 여기에 구현할 수 있습니다.
        }
      }
    }
  }
}
