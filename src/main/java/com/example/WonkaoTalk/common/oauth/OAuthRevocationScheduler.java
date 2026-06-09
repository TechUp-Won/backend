package com.example.WonkaoTalk.common.oauth;

import com.example.WonkaoTalk.domain.auth.entity.OAuthRevocationFailure;
import com.example.WonkaoTalk.domain.auth.enums.FallbackStatus;
import com.example.WonkaoTalk.domain.auth.repo.OAuthRevocationFailureRepo;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthRevocationScheduler {

  private static final int MAX_RETRY_COUNT = 5;
  private final OAuthRevocationFailureRepo failureRepo;
  private final OAuthRevocationAsyncHandler asyncHandler;

  @Scheduled(cron = "0 0/10 * * * *")
  public void processRevocationFailures() {
    List<OAuthRevocationFailure> pendingList =
        failureRepo.findTop50ByStatusOrderByCreatedAtAsc(FallbackStatus.PENDING);

    if (pendingList.isEmpty()) {
      return;
    }
    log.info("소셜 연동 해지 Fallback 배치를 시작합니다. 대상 건수: {}", pendingList.size());

    List<CompletableFuture<Void>> futures = pendingList.stream()
        .map(failure -> CompletableFuture.runAsync(() ->
                asyncHandler.processSingleFailure(
                    failure.getId()), // 새로운 스레드에서 영속성 컨텍스트를 새로 연다
            asyncHandler.getExecutor() // 커스텀 스레드 풀 사용
        ))
        .toList();

    // 모든 병렬 작업이 끝날 때까지 대기 (join)
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    log.info("소셜 연동 해지 Fallback 배치 완료.");
  }
}
