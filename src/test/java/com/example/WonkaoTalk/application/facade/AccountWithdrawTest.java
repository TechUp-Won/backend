package com.example.WonkaoTalk.application.facade;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

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
class AccountWithdrawTest {

  private final Long authId = 1L;
  private final String email = "test@test.com";
  private final String accessToken = "AccessTokenString";
  @InjectMocks
  private AccountWithdraw accountWithdraw;
  @Mock
  private UserService userService;
  @Mock
  private SellerService sellerService;
  @Mock
  private AuthService authService;
  @Mock
  private AuthCommandService authCommandService;

  @Test
  @DisplayName("일반 회원 탈퇴 시, 판매자 프로필이 없으면 auth까지 삭제된다.")
  public void withdrawUserOnlyUser() {
    //given
    given(sellerService.existsActiveSeller(authId)).willReturn(false);

    //when
    accountWithdraw.withdrawUser(authId, email, accessToken);

    //then
    then(userService).should(times(1)).withdrawUser(authId);
    then(authService).should(times(1)).invalidateToken(email, accessToken);
    then(authCommandService).should(times(1)).handleUserWithdraw(authId, false);
  }

  @Test
  @DisplayName("일반 회원 탈퇴 시, 판매자 프로필이 남아있으면 auth는 삭제되지 않는다.")
  public void withdrawUserActiveSeller() {
    //given
    given(sellerService.existsActiveSeller(authId)).willReturn(true);

    //when
    accountWithdraw.withdrawUser(authId, email, accessToken);

    //then
    then(userService).should(times(1)).withdrawUser(authId);
    then(authService).should(times(1)).invalidateToken(email, accessToken);
    then(authCommandService).should(times(1)).handleUserWithdraw(authId, true);
  }
}