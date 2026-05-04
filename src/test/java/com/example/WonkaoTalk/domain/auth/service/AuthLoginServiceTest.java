package com.example.WonkaoTalk.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.common.redis.RedisService;
import com.example.WonkaoTalk.domain.auth.dto.LoginRequest;
import com.example.WonkaoTalk.domain.auth.dto.TokenDto;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.repo.AuthLocalRepo;
import com.example.WonkaoTalk.domain.auth.repo.LoginHistoryRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthLoginServiceTest {

  @InjectMocks
  private AuthService authService;

  @Mock
  private AuthLocalRepo authLocalRepo;
  @Mock
  private UserRepo userRepo;
  @Mock
  private LoginHistoryRepo loginHistoryRepo;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private JwtTokenProvider jwtTokenProvider;
  @Mock
  private RedisService redisService;

  private MockHttpServletRequest httpRequest;

  @BeforeEach
  void setUp() {
    httpRequest = new MockHttpServletRequest();
    httpRequest.addHeader("User-Agent", "Test-Agent");
    httpRequest.setRemoteAddr("127.0.0.1");
  }

  @Test
  @DisplayName("정상적인 로그인 시도로 로그인에 성공한다.")
  public void loginSuccess() {
    //given
    LoginRequest request = new LoginRequest("test@test.com", "Qwer1234");
    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", 1L);
    AuthLocal authLocal = AuthLocal.builder()
        .email("test@test.com").passwordHash("encodedPassword").auth(auth).build();
    User user = User.builder().build();

    given(authLocalRepo.findByEmail(anyString())).willReturn(Optional.of(authLocal));
    given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
    given(userRepo.findByAuth(any(Auth.class))).willReturn(Optional.of(user));
    given(jwtTokenProvider.createAccessToken(anyString(), any(Long.class), anyString()))
        .willReturn("mockAccessToken");
    given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("mockRefreshToken");
    given(jwtTokenProvider.getRefreshTokenValidTime()).willReturn(1209600000L);

    //when
    TokenDto response = authService.login(request, httpRequest);

    //then
    assertThat(response.accessToken()).isEqualTo("mockAccessToken");

    // Redis 검증
    verify(redisService).setValues(
        eq("RT:test@test.com"), eq("mockRefreshToken"), any(Duration.class));
    verify(loginHistoryRepo).save(any());

  }

  @Test
  @DisplayName("비밀번호가 일치하지 않아 로그인에 실패하고 예외 발생")
  public void loginFailPasswordMismatch() {
    //given
    LoginRequest request = new LoginRequest("test@test.com", "wrongPassword");
    Auth auth = Auth.builder().role(Role.USER).build();
    AuthLocal authLocal = AuthLocal.builder()
        .email("test@test.com").passwordHash("encodedPassword").auth(auth).build();

    given(authLocalRepo.findByEmail(anyString())).willReturn(Optional.of(authLocal));
    given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

    //when & then
    assertThatThrownBy(() -> authService.login(request, httpRequest))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining(ErrorCode.AUTH_MISMATCH_PASSWORD.getMessage());

    verify(loginHistoryRepo).save(any());
  }

  @Test
  @DisplayName("정상적으로 로그아웃을 수행하여 Redis에서 토큰을 삭제한다.")
  public void logoutSuccess() {
    //given
    String email = "test@test.com";
    String accessToken = "mockAccessToken";
    given(redisService.hasKey("RT:" + email)).willReturn(true);
    given(jwtTokenProvider.getExpiration(accessToken)).willReturn(1800000L);
    //when
    authService.logout(accessToken, email);

    //then
    verify(redisService).deleteValues("RT:" + email);
    verify(redisService).setValues(eq("BlackList:" + accessToken), eq("logout"),
        any(Duration.class));
  }
}