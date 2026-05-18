package com.example.WonkaoTalk.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
import com.example.WonkaoTalk.domain.auth.enums.LoginStatus;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.user.entity.User;
import java.time.Duration;
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
class AuthServiceTest {

  @InjectMocks
  private AuthService authService;

  @Mock
  private AuthCommandService authCommandService;
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
  @DisplayName("사용 가능한 이메일 중복 검사")
  public void checkEmailAvailable() {
    //given
    EmailCheckRequest request = new EmailCheckRequest("test@test.com");
    given(authCommandService.existsByEmail(request.email())).willReturn(false);

    //when
    EmailCheckResponse response = authService.validateEmail(request);

    //then
    assertThat(response.isValid()).isTrue();
  }

  @Test
  @DisplayName("인증 계정 생성 성공")
  public void createAuthSuccess() {
    //given
    String email = "test@test.com";
    String password = "Qwer1234";
    Role role = Role.USER;

    given(authCommandService.existsByEmail(email)).willReturn(false);
    given(passwordEncoder.encode(password)).willReturn("EncodedPassword");

    Auth auth = Auth.builder().role(role).build();
    given(authCommandService.saveAuthLocal(email, "EncodedPassword", role)).willReturn(auth);

    //when
    Auth savedAuth = authService.createAuthLocal(email, password, role);

    //then
    assertThat(savedAuth).isNotNull();
    assertThat(savedAuth.getRole()).isEqualTo(Role.USER);
    verify(authCommandService).saveAuthLocal(email, "EncodedPassword", role);
  }

  @Test
  @DisplayName("중복된 이메일로 계정 생성 시 예외 발생")
  public void createAuthFailedByDuplicateEmail() {
    //given
    String email = "test@test.com";
    given(authCommandService.existsByEmail(email)).willReturn(true);

    //when & then
    BusinessException e = assertThrows(BusinessException.class, () -> {
      authService.createAuthLocal(email, "password", Role.USER);
    });

    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_DUPLICATE_EMAIL);
  }

  @Test
  @DisplayName("정상적인 로그인 시도로 로그인에 성공한다.")
  public void loginSuccess() {
    //given
    LoginRequest request = new LoginRequest("test@test.com", "Qwer1234");

    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", 1L);

    AuthLocal authLocal = AuthLocal.builder()
        .email("test@test.com")
        .passwordHash("encodedPassword")
        .auth(auth)
        .build();

    User user = User.builder().nickname("침착맨").build();

    given(authCommandService.getAuthLocalByEmail(anyString())).willReturn(authLocal);
    given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

    given(authCommandService.extractProfileNameByRole(any(Auth.class))).willReturn("침착맨");
    given(authCommandService.extractUserIdIfPresent(any(Auth.class))).willReturn(1L);
    given(authCommandService.extractSellerIdIfPresent(any(Auth.class))).willReturn(null);

    given(jwtTokenProvider.createAccessToken(anyString(), any(Long.class), nullable(Long.class),
        nullable(Long.class), anyString())).willReturn("mockAccessToken");
    given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("mockRefreshToken");
    given(jwtTokenProvider.getRefreshTokenValidTime()).willReturn(1209600000L);
    given(jwtTokenProvider.getAccessTokenValidTime()).willReturn(1800000L);

    //when
    TokenDto response = authService.login(request, httpRequest);

    //then
    assertThat(response).isNotNull();
    assertThat(response.accessToken()).isEqualTo("mockAccessToken");
    assertThat(response.profileName()).isEqualTo("침착맨");

    // Redis 검증
    verify(redisService).setValues(eq("RT:test@test.com"), eq("mockRefreshToken"),
        any(Duration.class));
    verify(authCommandService).saveLoginHistory(eq(auth), eq(LoginStatus.SUCCESS), anyString(),
        anyString());

  }

  @Test
  @DisplayName("비밀번호가 일치하지 않아 로그인에 실패하고 예외 발생")
  public void loginFailPasswordMismatch() {
    //given
    LoginRequest request = new LoginRequest("test@test.com", "wrongPassword");
    Auth auth = Auth.builder().role(Role.USER).build();
    AuthLocal authLocal = AuthLocal.builder()
        .email("test@test.com").passwordHash("encodedPassword").auth(auth).build();

    given(authCommandService.getAuthLocalByEmail(anyString())).willReturn(authLocal);
    given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

    //when & then
    assertThatThrownBy(() -> authService.login(request, httpRequest))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining(ErrorCode.AUTH_MISMATCH_PASSWORD.getMessage());

    verify(authCommandService).saveLoginHistory(eq(auth), eq(LoginStatus.FAILURE), anyString(),
        anyString());
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

  @Test
  @DisplayName("토큰 재발급 성공")
  public void reissueTokenSuccess() {
    //given
    String oldRefreshToken = "oldRefreshToken";
    String email = "test@test.com";
    String redisKey = "RT:" + email;

    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", 1L);
    AuthLocal authLocal = AuthLocal.builder().email(email).passwordHash("Qwer1234").auth(auth)
        .build();
    TokenDto newToken = TokenDto.of("newAccessToken", "newRefreshToken", 1000L, 9999L, auth,
        "프로필명");

    given(jwtTokenProvider.validateToken(oldRefreshToken)).willReturn(true);
    given(jwtTokenProvider.getEmailFromToken(oldRefreshToken)).willReturn(email);
    given(redisService.getValues(redisKey)).willReturn(oldRefreshToken);
    given(authCommandService.getAuthLocalByEmail(email)).willReturn(authLocal);
    given(authCommandService.extractProfileNameByRole(auth)).willReturn("프로필명");
    given(authCommandService.extractUserIdIfPresent(auth)).willReturn(1L);
    given(authCommandService.extractSellerIdIfPresent(auth)).willReturn(null);
    given(jwtTokenProvider.createRefreshToken(email)).willReturn(newToken.refreshToken());
    given(jwtTokenProvider.createAccessToken(anyString(), any(Long.class), nullable(Long.class),
        nullable(Long.class), eq(auth.getRole().name()))).willReturn(newToken.accessToken());
    given(jwtTokenProvider.getRefreshTokenValidTime()).willReturn(9999L);
    given(jwtTokenProvider.getAccessTokenValidTime()).willReturn(1000L); // 만료 시간 모킹

    //when
    TokenDto result = authService.reissueToken(oldRefreshToken);

    //then
    assertThat(result.accessToken()).isEqualTo("newAccessToken");
    assertThat(result.refreshToken()).isEqualTo("newRefreshToken");
    verify(redisService).setValues(eq(redisKey), eq("newRefreshToken"),
        eq(Duration.ofMillis(9999L)));
  }
}