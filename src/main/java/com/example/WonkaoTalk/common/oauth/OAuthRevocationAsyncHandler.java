package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.domain.auth.entity.OAuthRevocationFailure;
import com.example.WonkaoTalk.domain.auth.enums.FallbackStatus;
import com.example.WonkaoTalk.domain.auth.repo.OAuthRevocationFailureRepo;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthRevocationAsyncHandler {

  public static final int MAX_RETRY_COUNT = 5;
  private final OAuthRevocationFailureRepo failureRepo;
  private final List<OAuthRevocationProvider> revocationProviders;
  @Qualifier("revocationExecutor")
  private final Executor executor;

  public Executor getExecutor() {
    return this.executor;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processSingleFailure(Long failureId) {
    // 새로운 트랜잭션에서 영속성 관리를 위한 엔티티 재조회
    OAuthRevocationFailure failure = failureRepo.findById(failureId).orElse(null);
    if (failure == null || failure.getStatus() != FallbackStatus.PENDING) {
      return;
    }

    OAuthRevocationProvider providerClient = revocationProviders.stream()
        .filter(provider -> provider.supports(failure.getProvider()))
        .findFirst()
        .orElse(null);

    if (providerClient == null) {
      failure.incrementRetryCount(0);
      return;
    }

    try {
      providerClient.revoke(failure.getProviderUserId(), failure.getProviderRefreshToken());
      failure.markAsSuccess();

    } catch (Exception e) {
      failure.incrementRetryCount(MAX_RETRY_COUNT);
      log.warn("해지 재시도 실패. Failure ID: {}", failureId);

      // 영구 실패 상태가 되었을 때의 컴플라이언스 대응
      if (failure.getStatus() == FallbackStatus.PERMANENT_FAILURE) {
        // TODO: ApplicationEventPublisher를 이용해 '수동 연동 해제 안내 이메일 발송' 이벤트 발행 등
      }
    }
  }
}
