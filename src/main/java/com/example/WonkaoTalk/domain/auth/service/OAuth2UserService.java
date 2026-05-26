package com.example.WonkaoTalk.domain.auth.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.dto.CustomOAuth2User;
import com.example.WonkaoTalk.domain.auth.dto.GoogleUserInfo;
import com.example.WonkaoTalk.domain.auth.dto.NaverUserInfo;
import com.example.WonkaoTalk.domain.auth.dto.OAuth2UserInfo;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import com.example.WonkaoTalk.domain.auth.enums.AccountStatus;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.event.OAuth2UserCreatedEvent;
import com.example.WonkaoTalk.domain.auth.repo.AuthLocalRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthSocialRepo;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {

  private final AuthRepo authRepo;
  private final AuthLocalRepo authLocalRepo;
  private final AuthSocialRepo authSocialRepo;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);
    OAuth2UserInfo userInfo = extractUserInfo(userRequest, oAuth2User);
    Auth auth = getOrRegisterUser(userInfo);

    AuthProvider provider = AuthProvider.valueOf(userInfo.getProvider().toUpperCase());
    String providerId = userInfo.getProviderId();

    return new CustomOAuth2User(auth, oAuth2User.getAttributes(), provider, providerId);
  }

  private OAuth2UserInfo extractUserInfo(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
    String registrationId = userRequest.getClientRegistration().getRegistrationId();

    if (registrationId.equals("google")) {
      return new GoogleUserInfo(oAuth2User.getAttributes());
    } else if (registrationId.equals("naver")) {
      return new NaverUserInfo(oAuth2User.getAttributes());
    } else {
      throw new BusinessException(ErrorCode.OAUTH_INVALID_PROVIDER);
    }
  }

  private Auth getOrRegisterUser(OAuth2UserInfo userInfo) {
    AuthProvider provider = AuthProvider.valueOf(userInfo.getProvider().toUpperCase());
    String providerId = userInfo.getProviderId();
    String email = userInfo.getEmail();

    Optional<AuthSocial> optionalSocial = authSocialRepo.findByProviderAndProviderUserIdWithAuth(
        provider, providerId);
    if (optionalSocial.isPresent()) {
      return optionalSocial.get().getAuth();
    }

    Optional<AuthLocal> optionalLocal = authLocalRepo.findByEmail(email);
    if (optionalLocal.isPresent()) {
      Auth auth = optionalLocal.get().getAuth();
      saveAuthSocial(auth, provider, providerId, email);
      return auth;
    }

    Auth newAuth = Auth.builder().role(Role.USER).status(AccountStatus.ACTIVE).build();
    authRepo.save(newAuth);

    saveAuthSocial(newAuth, provider, providerId, email);

    eventPublisher.publishEvent(
        new OAuth2UserCreatedEvent(newAuth, userInfo.getEmail(), userInfo.getName()));

    return newAuth;
  }

  private void saveAuthSocial(Auth auth, AuthProvider provider, String providerId, String email) {
    AuthSocial authSocial = AuthSocial.builder()
        .auth(auth)
        .providerUserId(providerId)
        .provider(provider)
        .email(email)
        .build();
    authSocialRepo.save(authSocial);
  }
}
