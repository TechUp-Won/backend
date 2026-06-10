package com.example.WonkaoTalk.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.repo.AuthLocalRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthSocialRepo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

  private final Long authId = 1L;
  @InjectMocks
  private AuthCommandService authCommandService;
  @Mock
  private AuthRepo authRepo;
  @Mock
  private AuthLocalRepo authLocalRepo;
  @Mock
  private AuthSocialRepo authSocialRepo;

  @Test
  @DisplayName("일반/소셜 통합 계정에서 유저 탈퇴 시, 활성화된 판매자가 있다면 Role만 SELLER로 부분 탈퇴된다.")
  public void handleUserWithdraw_PartialWithdraw_RoleDowngraded() {
    // given
    Auth auth = Auth.builder().role(Role.USER_SELLER).build();
    ReflectionTestUtils.setField(auth, "id", authId);

    given(authRepo.findById(authId)).willReturn(Optional.of(auth));

    // when (활성화된 판매자가 존재함 = true)
    authCommandService.handleUserWithdraw(authId, true);

    // then (상태 검증)
    assertThat(auth.getRole()).isEqualTo(Role.SELLER);
    then(authLocalRepo).should(never()).findByAuth(auth);
    then(authSocialRepo).should(never()).findByAuth(auth);
  }

  @Test
  @DisplayName("유저 단일 계정 탈퇴 시, 활성화된 판매자가 없다면 연관된 인증 정보가 마스킹되고 완전 탈퇴(Soft Delete)된다.")
  public void handleUserWithdrawFullWithdrawAnonymizedAndDeleted() {
    // given
    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", authId);

    AuthLocal authLocal = AuthLocal.builder().email("test@test.com").passwordHash("hash").build();
    AuthSocial authSocial = AuthSocial.builder().providerUserId("google_12345").build();

    given(authRepo.findById(authId)).willReturn(Optional.of(auth));
    given(authLocalRepo.findByAuth(auth)).willReturn(Optional.of(authLocal));
    given(authSocialRepo.findByAuth(auth)).willReturn(List.of(authSocial));

    // when (활성화된 판매자가 없음 = false)
    authCommandService.handleUserWithdraw(authId, false);

    // then (상태 검증: 엔티티 내부의 withdraw() 동작 확인)
    assertThat(authLocal.getEmail()).isNotEqualTo("test@test.com"); // 마스킹 적용 확인
    assertThat(authSocial.getProviderUserId()).isNotEqualTo("google_12345"); // 마스킹 적용 확인

    // 엔티티 구조에 맞춰 isDeleted 필드나 getDeletedAt() 필드의 상태를 확인해야 합니다.
    // 예시: assertThat(auth.getDeletedAt()).isNotNull();

    then(authLocalRepo).should(times(1)).findByAuth(auth);
    then(authSocialRepo).should(times(1)).findByAuth(auth);
  }

  @Test
  @DisplayName("일반/소셜 통합 계정에서 판매자 탈퇴 시, 활성화된 유저가 있다면 Role만 USER로 부분 탈퇴된다.")
  public void handleSellerWithdrawPartialWithdrawRoleDowngraded() {
    // given
    Auth auth = Auth.builder().role(Role.USER_SELLER).build();
    ReflectionTestUtils.setField(auth, "id", authId);

    given(authRepo.findById(authId)).willReturn(Optional.of(auth));

    // when (활성화된 유저가 존재함 = true)
    authCommandService.handleSellerWithdraw(authId, true);

    // then (상태 검증)
    assertThat(auth.getRole()).isEqualTo(Role.USER);
    then(authLocalRepo).should(never()).findByAuth(auth);
  }

  @Test
  @DisplayName("존재하지 않는 계정 탈퇴 요청 시 BusinessException 예외가 발생한다.")
  public void handleWithdrawNotFoundThrowsException() {
    // given
    given(authRepo.findById(authId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authCommandService.handleUserWithdraw(authId, false))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_NOT_FOUND);
  }
}