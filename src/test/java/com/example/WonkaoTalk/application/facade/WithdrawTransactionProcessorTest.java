package com.example.WonkaoTalk.application.facade;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.service.AuthCommandService;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import com.example.WonkaoTalk.domain.seller.service.SellerService;
import com.example.WonkaoTalk.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WithdrawTransactionProcessorTest {

  @InjectMocks
  private WithdrawTransactionProcessor withdrawTransactionProcessor;

  @Mock
  private UserService userService;

  @Mock
  private AuthService authService;

  @Mock
  private AuthCommandService authCommandService;

  @Mock
  private SellerService sellerService;

  @Test
  @DisplayName("회원 탈퇴 DB 트랜잭션 - 일반 사용자 회원 탈퇴 성공")
  public void processWithdrawSuccessGeneralUser() {
    // given
    Long authId = 1L;
    given(sellerService.existsActiveSeller(authId)).willReturn(false);

    // when
    withdrawTransactionProcessor.withdrawUser(authId);

    // then
    verify(userService).withdrawUser(authId);
    verify(authCommandService).handleUserWithdraw(authId, false);
  }

  @Test
  @DisplayName("회원 탈퇴 DB 트랜잭션 - USER_SELLER 회원의 일반 사용자 탈퇴 성공 (권한 강등)")
  public void processWithdrawSuccessUserSeller() {
    // given
    Long authId = 1L;
    given(sellerService.existsActiveSeller(authId)).willReturn(true);

    // when
    withdrawTransactionProcessor.withdrawUser(authId);

    // then
    verify(userService).withdrawUser(authId);
    verify(authCommandService).handleUserWithdraw(authId, true);
  }

  @Test
  @DisplayName("회원 탈퇴 DB 트랜잭션 - 이미 탈퇴 처리된 계정의 중복 탈퇴 시도 차단")
  public void processWithdrawFailAlreadyDeleted() {
    // given
    Long userId = 1L;
    doThrow(new BusinessException(ErrorCode.USER_NOT_FOUND))
        .when(userService).withdrawUser(userId);

    // when & then
    assertThrows(BusinessException.class,
        () -> withdrawTransactionProcessor.withdrawUser(userId));
  }
}