package com.example.WonkaoTalk.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.dto.OAuth.CustomOAuth2User;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import com.example.WonkaoTalk.domain.auth.event.OAuth2UserCreatedEvent;
import com.example.WonkaoTalk.domain.auth.repo.AuthLocalRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthSocialRepo;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2UserServiceTest {

  @InjectMocks
  private OAuth2UserService oauth2UserService;

  @Mock
  private AuthRepo authRepo;
  @Mock
  private AuthLocalRepo authLocalRepo;
  @Mock
  private AuthSocialRepo authSocialRepo;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  private OAuth2UserRequest createMockUserRequest(String provider) {
    ClientRegistration registration = ClientRegistration.withRegistrationId(provider)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .clientId("client")
        .tokenUri("http://token")
        .authorizationUri("http://auth")
        .redirectUri("http://localhost/login/oauth2/code/" + provider)
        .build();
    OAuth2UserRequest userRequest = mock(OAuth2UserRequest.class);
    given(userRequest.getClientRegistration()).willReturn(registration);
    return userRequest;
  }

  @Test
  @DisplayName("소셜 로그인 - 소셜 제공자로부터 이메일 정보를 받지 못한 경우 즉시 예외가 발생한다")
  void processOAuth2UserNullEmailThrowsException() {
    // given
    OAuth2UserRequest userRequest = createMockUserRequest("google");
    OAuth2User oAuth2User = mock(OAuth2User.class);
    given(oAuth2User.getAttributes()).willReturn(Map.of("sub", "providerId123"));

    // when & then
    assertThatThrownBy(() -> oauth2UserService.processOAuth2User(userRequest, oAuth2User))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.OAUTH_NULL_EMAIL);

    verify(authSocialRepo, never()).findByProviderAndProviderUserIdWithAuth(any(), any());
  }

  @Test
  @DisplayName("소셜 로그인 - 동일한 이메일의 기존 일반 회원이 존재할 경우 기존 Auth에 소셜 계정을 연동한다")
  void processOAuth2UserExistingLocalUserLinksSocialAccount() {
    // given
    String email = "test@test.com";
    OAuth2UserRequest userRequest = createMockUserRequest("google");
    OAuth2User oAuth2User = mock(OAuth2User.class);

    given(oAuth2User.getAttributes()).willReturn(Map.of(
        "sub", "providerId123",
        "email", email,
        "name", "Test User"
    ));

    Auth existingAuth = Auth.builder().build();
    ReflectionTestUtils.setField(existingAuth, "id", 1L);
    AuthLocal authLocal = AuthLocal.builder().auth(existingAuth).email(email).build();

    given(authSocialRepo.findByProviderAndProviderUserIdWithAuth(any(), any())).willReturn(
        Optional.empty());
    given(authLocalRepo.findByEmail(email)).willReturn(Optional.of(authLocal));

    // when
    OAuth2User resultUser = oauth2UserService.processOAuth2User(userRequest, oAuth2User);

    // then
    assertThat(resultUser).isInstanceOf(CustomOAuth2User.class);

    verify(authRepo, never()).save(any());
    verify(authSocialRepo, times(1)).save(any(AuthSocial.class));
  }

  @Test
  @DisplayName("소셜 로그인 - 완전한 신규 사용자일 경우 새로운 Auth를 생성하고 이벤트를 발행한다")
  void processOAuth2UserCompletelyNewUserCreatesAuthAndPublishesEvent() {
    // given
    String email = "test@test.com";
    OAuth2UserRequest userRequest = createMockUserRequest("google");
    OAuth2User oAuth2User = mock(OAuth2User.class);

    given(oAuth2User.getAttributes()).willReturn(Map.of(
        "sub", "providerId123",
        "email", email,
        "name", "New User"
    ));

    given(authSocialRepo.findByProviderAndProviderUserIdWithAuth(any(), any())).willReturn(
        Optional.empty());
    given(authLocalRepo.findByEmail(email)).willReturn(Optional.empty());
    given(authSocialRepo.findFirstByEmail(email)).willReturn(Optional.empty());

    // when
    OAuth2User resultUser = oauth2UserService.processOAuth2User(userRequest, oAuth2User);

    // then
    assertThat(resultUser).isInstanceOf(CustomOAuth2User.class);

    verify(authRepo, times(1)).save(any(Auth.class));
    verify(authSocialRepo, times(1)).save(any(AuthSocial.class));
    verify(eventPublisher, times(1)).publishEvent(any(OAuth2UserCreatedEvent.class));
  }

  @Test
  @DisplayName("제공자 식별 분기 - 네이버(naver) 로그인이 들어오면 NaverUserInfo 객체로 래핑된다")
  public void extractUserInfoNaverProviderReturnsNaverUserInfo() {
    // given
    OAuth2UserRequest userRequest = createMockUserRequest("naver");
    OAuth2User oAuth2User = mock(OAuth2User.class);
    given(oAuth2User.getAttributes()).willReturn(
        Map.of("response", Map.of("id", "naver_123", "email", "n@n.com")));

    // when
    Object userInfo = ReflectionTestUtils.invokeMethod(oauth2UserService, "extractUserInfo",
        userRequest, oAuth2User);

    // then
    assertThat(userInfo.getClass().getSimpleName()).isEqualTo("NaverUserInfo");
  }

  @Test
  @DisplayName("제공자 식별 분기 - 지원하지 않는 제공자(kakao 등)일 경우 예외가 발생한다")
  public void extractUserInfoInvalidProviderThrowsException() {
    // given
    OAuth2UserRequest userRequest = createMockUserRequest("kakao");
    OAuth2User oAuth2User = mock(OAuth2User.class);

    // when & then
    assertThatThrownBy(
        () -> ReflectionTestUtils.invokeMethod(oauth2UserService, "extractUserInfo", userRequest,
            oAuth2User))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.OAUTH_INVALID_PROVIDER);
  }

  @Test
  @DisplayName("소셜 연동 병합 분기 - AuthLocal이 없고 다른 AuthSocial만 존재할 경우 해당 Auth에 병합된다")
  public void processOAuth2UserNoLocalButExistingSocialLinksAccount() {
    // given
    String email = "test@test.com";
    OAuth2UserRequest userRequest = createMockUserRequest("naver");
    OAuth2User oAuth2User = mock(OAuth2User.class);
    given(oAuth2User.getAttributes()).willReturn(
        Map.of("response", Map.of("id", "naver_123", "email", email)));

    Auth existingAuth = Auth.builder().build();
    ReflectionTestUtils.setField(existingAuth, "id", 77L);
    AuthSocial existingSocial = AuthSocial.builder().auth(existingAuth).email(email).build();

    given(authSocialRepo.findByProviderAndProviderUserIdWithAuth(any(), any())).willReturn(
        Optional.empty());
    given(authLocalRepo.findByEmail(email)).willReturn(Optional.empty());
    given(authSocialRepo.findFirstByEmail(email)).willReturn(Optional.of(existingSocial));

    // when
    OAuth2User result = oauth2UserService.processOAuth2User(userRequest, oAuth2User);

    // then
    verify(authSocialRepo, times(1)).save(any(AuthSocial.class));
  }
}