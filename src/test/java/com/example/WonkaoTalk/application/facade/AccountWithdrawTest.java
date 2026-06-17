package com.example.WonkaoTalk.application.facade;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.example.WonkaoTalk.common.oauth.OAuthRevocationClient;
import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import com.example.WonkaoTalk.domain.auth.service.AuthCommandService;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
  private AuthService authService;
  @Mock
  private AuthCommandService authCommandService;
  @Mock
  private OAuthRevocationClient oAuthRevocationClient;
  @Mock
  private WithdrawTransactionProcessor withdrawProcessor;

  private List<AuthSocial> linkedSocials = new ArrayList<>();

  @BeforeEach
  void setUp() {
    AuthSocial authSocial = AuthSocial.builder()
        .provider(AuthProvider.GOOGLE)
        .providerUserId("provider_user_id")
        .providerRefreshToken("provider_refresh_token")
        .email("test@test.com")
        .build();
    this.linkedSocials.add(authSocial);
  }

  @Test
  @DisplayName("사용자 탈퇴 요청 시 연동 해제, 내부 트랜잭션, 토큰 무효화가 순차적으로 실행된다.")
  public void withdrawUserSuccess() {
    // given
    given(authCommandService.getLinkedSocials(authId)).willReturn(linkedSocials);

    // when
    accountWithdraw.withdrawUser(authId, email, accessToken);

    // then
    then(oAuthRevocationClient).should(times(1)).revokeIfSocialAccountExists(authId, linkedSocials);
    then(withdrawProcessor).should(times(1)).withdrawUser(authId);
    then(authService).should(times(1)).invalidateToken(email, accessToken);
  }

  @Test
  @DisplayName("판매자 탈퇴 요청 시 연동 해제, 내부 트랜잭션, 토큰 무효화가 순차적으로 실행된다.")
  public void withdrawSellerSuccess() {
    // given
    given(authCommandService.getLinkedSocials(authId)).willReturn(linkedSocials);
    
    // when
    accountWithdraw.withdrawSeller(authId, email, accessToken);

    // then
    then(oAuthRevocationClient).should(times(1)).revokeIfSocialAccountExists(authId, linkedSocials);
    then(withdrawProcessor).should(times(1)).withdrawSeller(authId);
    then(authService).should(times(1)).invalidateToken(email, accessToken);
  }
}