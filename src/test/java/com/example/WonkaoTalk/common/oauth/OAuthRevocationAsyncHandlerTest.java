package com.example.WonkaoTalk.common.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import com.example.WonkaoTalk.domain.auth.entity.OAuthRevocationFailure;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import com.example.WonkaoTalk.domain.auth.enums.FallbackStatus;
import com.example.WonkaoTalk.domain.auth.repo.OAuthRevocationFailureRepo;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuthRevocationAsyncHandlerTest {

  private OAuthRevocationAsyncHandler asyncHandler;

  @Mock
  private OAuthRevocationFailureRepo failureRepo;
  @Mock
  private OAuthRevocationProvider providerClient;
  @Mock
  private Executor executor;

  @BeforeEach
  void setUp() {
    asyncHandler = new OAuthRevocationAsyncHandler(failureRepo, List.of(providerClient), executor);
  }

  @Test
  @DisplayName("연동 해제 스케줄러 - 외부 API 통신 실패 시 재시도 횟수가 증가하고 상태는 PENDING으로 유지된다")
  void processSingleFailure_ApiThrowsException_IncrementsRetryCount() {
    // given
    Long failureId = 1L;
    OAuthRevocationFailure failure = OAuthRevocationFailure.builder()
        .provider(AuthProvider.GOOGLE)
        .providerUserId("provider_id")
        .providerRefreshToken("refresh_token")
        .build();

    ReflectionTestUtils.setField(failure, "retryCount", 0);

    given(failureRepo.findById(failureId)).willReturn(Optional.of(failure));
    given(providerClient.supports(AuthProvider.GOOGLE)).willReturn(true);

    // 통신 시 예외 발생 시뮬레이션
    doThrow(new RuntimeException("API Timeout")).when(providerClient).revoke(any(), any());

    // when
    asyncHandler.processSingleFailure(failureId);

    // then
    assertThat(failure.getStatus()).isEqualTo(FallbackStatus.PENDING);
    assertThat(failure.getRetryCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("연동 해제 스케줄러 - 최대 재시도 횟수 초과 시 상태가 PERMANENT_FAILURE로 변경된다")
  void processSingleFailure_MaxRetryExceeded_ChangesStatusToPermanentFailure() {
    // given
    Long failureId = 1L;
    OAuthRevocationFailure failure = OAuthRevocationFailure.builder()
        .provider(AuthProvider.GOOGLE)
        .providerUserId("provider_id")
        .providerRefreshToken("refresh_token")
        .build();

    // 재시도 횟수를 임계치(MAX_RETRY_COUNT - 1)로 설정하여, 다음 실패 시 상태가 변경되도록 유도
    ReflectionTestUtils.setField(failure, "retryCount",
        OAuthRevocationAsyncHandler.MAX_RETRY_COUNT - 1);

    given(failureRepo.findById(failureId)).willReturn(Optional.of(failure));
    given(providerClient.supports(AuthProvider.GOOGLE)).willReturn(true);
    doThrow(new RuntimeException("API Timeout")).when(providerClient).revoke(any(), any());

    // when
    asyncHandler.processSingleFailure(failureId);

    // then
    assertThat(failure.getStatus()).isEqualTo(FallbackStatus.PERMANENT_FAILURE);
    assertThat(failure.getRetryCount()).isEqualTo(OAuthRevocationAsyncHandler.MAX_RETRY_COUNT);
  }
}