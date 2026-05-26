package com.example.WonkaoTalk.common.config.security;

import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.redis.RedisService;
import com.example.WonkaoTalk.domain.auth.dto.CustomOAuth2User;
import com.example.WonkaoTalk.domain.auth.dto.SocialLoginDto;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import com.example.WonkaoTalk.domain.auth.service.AuthCommandService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final AuthCommandService authCommandService;
  private final JwtTokenProvider jwtTokenProvider;
  private final RedisService redisService;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException {

    CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
    String email = (String) oAuth2User.getAttributes().get("email");
    AuthProvider provider = oAuth2User.getProvider();
    String providerId = oAuth2User.getProviderId();
    
    SocialLoginDto userInfo = authCommandService.generateSocialLoginData(email, provider,
        providerId);
    String accessToken = jwtTokenProvider.createAccessToken(
        email,
        userInfo.authId(),
        userInfo.userId(),
        userInfo.sellerId(),
        userInfo.role().name());
    String refreshToken = jwtTokenProvider.createRefreshToken(email);

    redisService.setValues("RT:" + email, refreshToken,
        Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidTime()));

    response.addCookie(createCookie("refresh-token", refreshToken, true,
        (int) (jwtTokenProvider.getRefreshTokenValidTime() / 1000)));
    response.addCookie(createCookie("access-token", accessToken, false, 60));

    getRedirectStrategy().sendRedirect(request, response, "http://localhost:3000");
  }

  private Cookie createCookie(String name, String value, boolean httpOnly, int maxAge) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(httpOnly);
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);
    return cookie;
  }
}
