package com.example.WonkaoTalk.domain.auth.controller;

import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.redis.RedisService;
import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.AuthUserInfoDto;
import com.example.WonkaoTalk.domain.auth.dto.LoginResponse;
import com.example.WonkaoTalk.domain.auth.dto.MockSocialLoginRequest;
import com.example.WonkaoTalk.domain.auth.dto.SocialLoginDto;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.service.AuthCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * local 프로필 전용 Mock 소셜 로그인 엔드포인트. 부하테스트(k6) 등에서 실제 OAuth2 핸드셰이크 없이
 * 소셜 로그인 트래픽 비율을 검증하기 위한 용도이며, dev/prod 프로필에서는 빈 자체가 등록되지 않는다.
 */
@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Profile("local")
@Tag(name = "인증(local 전용)", description = "부하테스트용 Mock 소셜 로그인 API")
public class MockAuthController {

  private final AuthCommandService authCommandService;
  private final JwtTokenProvider jwtTokenProvider;
  private final RedisService redisService;

  @Operation(summary = "[local 전용] Mock 소셜 로그인",
      description = "실제 OAuth2 핸드셰이크 없이 provider/providerId/email만으로 소셜 로그인을 흉내냅니다.")
  @PostMapping("/social-login")
  public ResponseEntity<ApiResponse<LoginResponse>> mockSocialLogin(
      @Valid @RequestBody MockSocialLoginRequest request
  ) {
    SocialLoginDto loginData = authCommandService.getOrCreateMockSocialLogin(
        request.provider(), request.providerId(), request.email());
    Auth auth = authCommandService.getAuthById(loginData.authId());
    AuthUserInfoDto userInfo = authCommandService.getAuthUserInfo(auth);

    String accessToken = jwtTokenProvider.createAccessToken(
        request.email(), loginData.authId(), loginData.userId(), loginData.sellerId(),
        loginData.role().name());
    String refreshToken = jwtTokenProvider.createRefreshToken(request.email());

    redisService.setValues("RT:" + request.email(), refreshToken,
        Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidTime()));

    ResponseCookie refreshCookie = ResponseCookie.from("refresh-token", refreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(jwtTokenProvider.getRefreshTokenValidTime() / 1000)
        .sameSite("Strict")
        .build();

    LoginResponse responseBody = LoginResponse.of(accessToken,
        jwtTokenProvider.getAccessTokenValidTime(), auth, userInfo.profileName());

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(ApiResponse.success("Mock 소셜 로그인에 성공하였습니다.", responseBody));
  }
}
