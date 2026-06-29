package com.example.WonkaoTalk.domain.auth.service;

import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.common.redis.RedisService;
import com.example.WonkaoTalk.domain.auth.dto.AuthIntegrationResultDto;
import com.example.WonkaoTalk.domain.auth.dto.AuthUserInfoDto;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckResponse;
import com.example.WonkaoTalk.domain.auth.dto.LoginRequest;
import com.example.WonkaoTalk.domain.auth.dto.TokenDto;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.enums.LoginStatus;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final AuthCommandService authCommandService;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final RedisService redisService;

  public EmailCheckResponse validateEmail(EmailCheckRequest request) {
    return EmailCheckResponse.from(!authCommandService.existsByEmail(request.email()));
  }

  // BCrypt 검증은 CPU 비용이 커서 톰캣 워커 스레드를 점유하지 않도록 별도 풀(bcryptExecutor)에서 처리한다.
  @Async("bcryptExecutor")
  public CompletableFuture<TokenDto> login(LoginRequest request, String userAgent,
      String ipAddress) {
    // TODO: 로그인 실패 횟수에 따른 계정 잠금이나 추가인증 기능 구현
    AuthLocal authLocal = authCommandService.getAuthLocalByEmail(request.email());
    Auth auth = authLocal.getAuth();

    if (!passwordEncoder.matches(request.password(), authLocal.getPasswordHash())) {
      authCommandService.saveLoginHistory(auth, LoginStatus.FAILURE, userAgent, ipAddress);
      throw new BusinessException(ErrorCode.AUTH_MISMATCH_PASSWORD);
    }

    TokenDto dto = publishToken(authLocal.getEmail(), auth);

    authCommandService.saveLoginHistory(auth, LoginStatus.SUCCESS, userAgent, ipAddress);

    return CompletableFuture.completedFuture(dto);
  }

  public TokenDto reissueToken(String refreshToken) {
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }

    String email = jwtTokenProvider.getEmailFromToken(refreshToken);
    String redisKey = "RT:" + email;
    String storedRefreshToken = redisService.getValues(redisKey);

    if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
      redisService.deleteValues(redisKey);
      throw new BusinessException(ErrorCode.AUTH_SUSPECT_THEFT_TOKEN);
    }

    AuthLocal authLocal = authCommandService.getAuthLocalByEmail(email);
    Auth auth = authLocal.getAuth();

    return publishToken(authLocal.getEmail(), auth);
  }

  public AuthIntegrationResultDto linkOrCreateLocal(String email, String password, Role role) {
    if (authCommandService.existsByEmail(email)) {
      throw new BusinessException(ErrorCode.AUTH_DUPLICATE_EMAIL);
    }
    String encodedPassword = passwordEncoder.encode(password);

    return authCommandService.linkOrCreateTransaction(email, encodedPassword, role);
  }

  private TokenDto publishToken(String email, Auth auth) {
    AuthUserInfoDto dto = authCommandService.getAuthUserInfo(auth);
    String role = auth.getRole().name();

    String accessToken = jwtTokenProvider.createAccessToken(email, auth.getId(), dto.userId(),
        dto.sellerId(), role);
    String refreshToken = jwtTokenProvider.createRefreshToken(email);

    long refreshExpirationTime = jwtTokenProvider.getRefreshTokenValidTime();
    long accessExpirationTime = jwtTokenProvider.getAccessTokenValidTime();

    redisService.setValues("RT:" + email, refreshToken, Duration.ofMillis(refreshExpirationTime));

    return TokenDto.of(accessToken, refreshToken, accessExpirationTime, refreshExpirationTime, auth,
        dto.profileName());
  }

  public void invalidateToken(String email, String accessToken) {
    String redisKey = "RT:" + email;
    if (redisService.hasKey(redisKey)) {
      redisService.deleteValues(redisKey);
    }

    Long expiration = jwtTokenProvider.getExpiration(accessToken);
    log.info("블랙리스트 등록 토큰: {}", accessToken);
    log.info("남은 만료 시간: {}", expiration);
    redisService.setValues("BlackList:" + accessToken, "logout", Duration.ofMillis(expiration));
  }
}
