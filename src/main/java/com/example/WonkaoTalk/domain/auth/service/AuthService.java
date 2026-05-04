package com.example.WonkaoTalk.domain.auth.service;

import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.common.redis.RedisService;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckResponse;
import com.example.WonkaoTalk.domain.auth.dto.LoginRequest;
import com.example.WonkaoTalk.domain.auth.dto.LoginResponse;
import com.example.WonkaoTalk.domain.auth.dto.SignUpRequest;
import com.example.WonkaoTalk.domain.auth.dto.SignUpResponse;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.entity.LoginHistory;
import com.example.WonkaoTalk.domain.auth.enums.LoginStatus;
import com.example.WonkaoTalk.domain.auth.repo.AuthLocalRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.auth.repo.LoginHistoryRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final AuthRepo authRepo;
  private final AuthLocalRepo authLocalRepo;
  private final LoginHistoryRepo loginHistoryRepo;
  private final UserRepo userRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final RedisService redisService;

  @Transactional(readOnly = true)
  public EmailCheckResponse validateEmail(EmailCheckRequest request) {
    boolean exists = authLocalRepo.existsByEmail(request.email());

    return EmailCheckResponse.from(!exists);
  }

  @Transactional
  public SignUpResponse signUp(SignUpRequest request) {
    if (!request.password().equals(request.passwordCheck())) {
      throw new BusinessException(ErrorCode.AUTH_MISMATCH_PASSWORD);
    }

    if (authLocalRepo.existsByEmail(request.email())) {
      throw new BusinessException(ErrorCode.AUTH_DUPLICATE_EMAIL);
    }

    Auth auth = Auth.builder().build();
    Auth savedAuth = authRepo.save(auth);

    String encodedPassword = passwordEncoder.encode(request.password());
    AuthLocal authLocal = AuthLocal.builder()
        .auth(savedAuth)
        .email(request.email())
        .passwordHash(encodedPassword)
        .failedAttemptsCount(0)
        .build();
    authLocalRepo.save(authLocal);

    User user = User.builder()
        .auth(savedAuth)
        .name(request.name())
        .nickname(request.nickname())
        .phone(request.phone())
        .birthDate(request.birthDate())
        .gender(request.gender())
        .build();
    User savedUser = userRepo.save(user);

    return SignUpResponse.builder()
        .authId(savedAuth.getId())
        .userId(savedUser.getId())
        .createdAt(savedAuth.getCreatedAt())
        .build();
  }

  @Transactional
  public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
    // TODO: 로그인 실패 횟수에 따른 계정 잠금이나 추가인증 기능 구현
    AuthLocal authLocal = authLocalRepo.findByEmail(request.email())
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_EMAIL));

    Auth auth = authLocal.getAuth();

    if (!passwordEncoder.matches(request.password(), authLocal.getPasswordHash())) {
      saveLoginHistory(auth, LoginStatus.FAILURE, httpRequest);
      throw new BusinessException(ErrorCode.AUTH_MISMATCH_PASSWORD);
    }

    User user = userRepo.findByAuth(auth)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    String email = authLocal.getEmail();

    String accessToken = jwtTokenProvider.createAccessToken(email, auth.getId(),
        auth.getRole().name());
    String refreshToken = jwtTokenProvider.createRefreshToken(email);

    long refreshExpirationTime = jwtTokenProvider.getRefreshTokenValidTime();
    redisService.setValues("RT:" + email, refreshToken,
        Duration.ofMillis(refreshExpirationTime)
    );

    saveLoginHistory(auth, LoginStatus.SUCCESS, httpRequest);

    long accessExpirationTime = jwtTokenProvider.getAccessTokenValidTime();
    return LoginResponse.of(accessToken, accessExpirationTime, auth, user);

  }

  private void saveLoginHistory(Auth auth, LoginStatus status, HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    String ipAddress = request.getHeader("X-Forwarded-For");
    // 프록시나 로드밸런서를 거쳤을 경우 대비
    if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
      ipAddress = request.getRemoteAddr();
    }

    LoginHistory history = LoginHistory.builder()
        .auth(auth)
        .ipAddress(ipAddress)
        .userAgent(userAgent)
        .status(status)
        .build();

    loginHistoryRepo.save(history);
  }

  @Transactional
  public void logout(String email) {
    String redisKey = "RT:" + email;

    if (redisService.hasKey(redisKey)) {
      redisService.deleteValues(redisKey);
    }
  }

}
