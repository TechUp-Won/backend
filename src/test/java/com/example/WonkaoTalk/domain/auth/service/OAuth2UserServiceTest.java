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
  void processOAuth2User_NullEmail_ThrowsException() {
    // given
    OAuth2UserRequest userRequest = createMockUserRequest("google");
    OAuth2User oAuth2User = mock(OAuth2User.class);

    // 이메일이 누락된 구글의 속성 맵을 모킹합니다 (sub만 존재)
    given(oAuth2User.getAttributes()).willReturn(Map.of("sub", "providerId123"));

    // when & then
    // 리플렉션 없이 동일 패키지 내의 default 메서드를 직접 호출합니다.
    assertThatThrownBy(() -> oauth2UserService.processOAuth2User(userRequest, oAuth2User))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.OAUTH_NULL_EMAIL);

    verify(authSocialRepo, never()).findByProviderAndProviderUserIdWithAuth(any(), any());
  }

  @Test
  @DisplayName("소셜 로그인 - 동일한 이메일의 기존 일반 회원이 존재할 경우 기존 Auth에 소셜 계정을 연동한다")
  void processOAuth2User_ExistingLocalUser_LinksSocialAccount() {
    // given
    String email = "test@test.com";
    OAuth2UserRequest userRequest = createMockUserRequest("google");
    OAuth2User oAuth2User = mock(OAuth2User.class);

    // 정상적인 이메일이 포함된 속성 맵
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
    assertThat(((CustomOAuth2User) resultUser).getAuth().getId()).isEqualTo(1L); // 기존 Auth가 반환됨

    verify(authRepo, never()).save(any()); // 신규 Auth 생성은 일어나지 않음
    verify(authSocialRepo, times(1)).save(any(AuthSocial.class)); // 소셜 연동 데이터만 저장됨
  }

  @Test
  @DisplayName("소셜 로그인 - 완전한 신규 사용자일 경우 새로운 Auth를 생성하고 이벤트를 발행한다")
  void processOAuth2User_CompletelyNewUser_CreatesAuthAndPublishesEvent() {
    // given
    String email = "new@test.com";
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

    // 신규 Auth 및 Social 정보가 저장되었는지 검증
    verify(authRepo, times(1)).save(any(Auth.class));
    verify(authSocialRepo, times(1)).save(any(AuthSocial.class));
    // 외부 프로필 생성을 위한 이벤트가 발행되었는지 검증
    verify(eventPublisher, times(1)).publishEvent(any(OAuth2UserCreatedEvent.class));
  }
}