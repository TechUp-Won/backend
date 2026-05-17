package com.example.WonkaoTalk.domain.auth.service;

import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.common.redis.RedisService;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckResponse;
import com.example.WonkaoTalk.domain.auth.dto.LoginRequest;
import com.example.WonkaoTalk.domain.auth.dto.TokenDto;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.entity.LoginHistory;
import com.example.WonkaoTalk.domain.auth.enums.LoginStatus;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.repo.AuthLocalRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.auth.repo.LoginHistoryRepo;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
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
  private final SellerRepo sellerRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final RedisService redisService;

  @Transactional(readOnly = true)
  public EmailCheckResponse validateEmail(EmailCheckRequest request) {
    boolean exists = authLocalRepo.existsByEmail(request.email());

    return EmailCheckResponse.from(!exists);
  }

  @Transactional
  public Auth createAuthLocal(String email, String password, Role role) {
    if (authLocalRepo.existsByEmail(email)) {
      throw new BusinessException(ErrorCode.AUTH_DUPLICATE_EMAIL);
    }

    Auth auth = Auth.builder()
        .role(role)
        .build();
    Auth savedAuth = authRepo.save(auth);

    String encodedPassword = passwordEncoder.encode(password);
    AuthLocal authLocal = AuthLocal.builder()
        .auth(savedAuth)
        .email(email)
        .passwordHash(encodedPassword)
        .failedAttemptsCount(0)
        .build();
    authLocalRepo.save(authLocal);

    return savedAuth;
  }

  @Transactional
  public TokenDto login(LoginRequest request, HttpServletRequest httpRequest) {
    // TODO: 로그인 실패 횟수에 따른 계정 잠금이나 추가인증 기능 구현
    AuthLocal authLocal = authLocalRepo.findByEmail(request.email())
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_EMAIL));

    Auth auth = authLocal.getAuth();

    if (!passwordEncoder.matches(request.password(), authLocal.getPasswordHash())) {
      saveLoginHistory(auth, LoginStatus.FAILURE, httpRequest);
      throw new BusinessException(ErrorCode.AUTH_MISMATCH_PASSWORD);
    }

    TokenDto dto = publishToken(authLocal.getEmail(), auth);

    saveLoginHistory(auth, LoginStatus.SUCCESS, httpRequest);

    return dto;

  }

  @Transactional
  public void logout(String accessToken, String email) {
    invalidateToken(email, accessToken);
  }

  @Transactional
  public void withdraw(Long authId) {
    Auth auth = authRepo.findById(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_NOT_FOUND));

    authLocalRepo.findByAuth(auth).ifPresent(AuthLocal::withdraw);

    auth.withdraw();
  }

  @Transactional
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

    AuthLocal authLocal = authLocalRepo.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_NOT_FOUND));
    Auth auth = authLocal.getAuth();

    return publishToken(authLocal.getEmail(), auth);
  }

  private void saveLoginHistory(Auth auth, LoginStatus status, HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    String ipAddress = request.getHeader("X-Forwarded-For");
    // 프록시나 로드밸런서를 거쳤을 경우 대비
    if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
      ipAddress = request.getRemoteAddr();
    } else {
      ipAddress = ipAddress.split(",")[0].trim();
    }

    LoginHistory history = LoginHistory.builder()
        .auth(auth)
        .ipAddress(ipAddress)
        .userAgent(userAgent)
        .status(status)
        .build();

    loginHistoryRepo.save(history);
  }

  private TokenDto publishToken(String email, Auth auth) {
    String profileName = extractProfileNameByRole(auth);
    String role = auth.getRole().name();
    Long userId = extractUserIdIfPresent(auth);
    Long sellerId = extractSellerIdIfPresent(auth);
    String accessToken =
        jwtTokenProvider.createAccessToken(email, auth.getId(), userId, sellerId, role);
    String refreshToken = jwtTokenProvider.createRefreshToken(email);

    long refreshExpirationTime = jwtTokenProvider.getRefreshTokenValidTime();
    long accessExpirationTime = jwtTokenProvider.getAccessTokenValidTime();

    redisService.setValues("RT:" + email, refreshToken,
        Duration.ofMillis(refreshExpirationTime)
    );

    return TokenDto.of(accessToken, refreshToken, accessExpirationTime, auth, profileName);
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

  private String extractProfileNameByRole(Auth auth) {
    Role role = auth.getRole();

    if (role == Role.USER || role == Role.USER_SELLER) {
      return userRepo.findByAuth(auth)
          .map(User::getNickname)
          .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    } else if (role == Role.SELLER) {
      return sellerRepo.findByAuth(auth)
          .map(Seller::getName)
          .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
    } else if (role == Role.ADMIN) {
      return "관리자";
    }
    return "알 수 없는 사용자";
  }

  private Long extractUserIdIfPresent(Auth auth) {
    Role role = auth.getRole();
    if (role == Role.USER || role == Role.USER_SELLER) {
      return userRepo.findByAuth(auth)
          .map(User::getId)
          .orElse(null);
    }
    return null;
  }

  private Long extractSellerIdIfPresent(Auth auth) {
    Role role = auth.getRole();
    if (role == Role.SELLER || role == Role.USER_SELLER) {
      return sellerRepo.findByAuth(auth)
          .map(Seller::getId)
          .orElse(null);
    }
    return null;
  }
}
