package com.example.WonkaoTalk.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.common.redis.RedisService;
import com.example.WonkaoTalk.domain.auth.dto.AuthUserInfoDto;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckResponse;
import com.example.WonkaoTalk.domain.auth.dto.LoginRequest;
import com.example.WonkaoTalk.domain.auth.dto.TokenDto;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.entity.LoginHistory;
import com.example.WonkaoTalk.domain.auth.enums.LoginStatus;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.repo.LoginHistoryRepo;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private final String email = "test@test.com";
  private final String password = "Qwer1234!";
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
  @Mock
  private LoginHistoryRepo loginHistoryRepo;

  @Captor
  private ArgumentCaptor<LoginHistory> loginHistoryCaptor;

  private MockHttpServletRequest httpRequest;

  @BeforeEach
  void setUp() {
    httpRequest = new MockHttpServletRequest();
    httpRequest.addHeader("User-Agent", "User-Agent");
    httpRequest.setRemoteAddr("127.0.0.1");
  }

  @Test
  @DisplayName("이메일 중복 검사 - 미가입 이메일 검사 성공")
  public void checkEmailAvailable() {
    //given
    EmailCheckRequest request = new EmailCheckRequest(email);
    given(authCommandService.existsByEmail(request.email())).willReturn(false);

    //when
    EmailCheckResponse response = authService.validateEmail(request);

    //then
    assertThat(response.isValid()).isTrue();
  }

  @Test
  @DisplayName("이메일 중복 검사 - 기가입 이메일 검사 성공")
  public void checkEmailDuplicated() {
    //given
    EmailCheckRequest request = new EmailCheckRequest(email);
    given(authCommandService.existsByEmail(request.email())).willReturn(true);

    //when
    EmailCheckResponse response = authService.validateEmail(request);

    //then
    assertThat(response.isValid()).isFalse();

  }

  @Test
  @DisplayName("회원가입 - 클라이언트 검증을 우회한 중복 가입 시도 실패")
  public void createAuthFailedByDuplicateEmail() {
    //given
    given(authCommandService.existsByEmail(email)).willReturn(true);

    //when & then
    BusinessException e = assertThrows(BusinessException.class, () -> {
      authService.createAuthLocal(email, "password", Role.USER);
    });

    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_DUPLICATE_EMAIL);
  }

  @Test
  @DisplayName("로그인 - 유효한 자격증명 로그인 및 이력 기록")
  public void loginSuccess() {
    //given
    LoginRequest request = new LoginRequest(email, password);
    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", 1L);
    AuthLocal authLocal = AuthLocal.builder().email(email).passwordHash("encoded").auth(auth)
        .build();
    AuthUserInfoDto userInfo = AuthUserInfoDto.of("침착맨", 1L, null);

    given(authCommandService.getAuthLocalByEmail(email)).willReturn(authLocal);
    given(passwordEncoder.matches(password, authLocal.getPasswordHash())).willReturn(true);
    given(authCommandService.getAuthUserInfo(auth)).willReturn(userInfo);

    given(jwtTokenProvider.createAccessToken(anyString(), any(Long.class), nullable(Long.class),
        nullable(Long.class), anyString())).willReturn("mockAccessToken");
    given(jwtTokenProvider.createRefreshToken(email)).willReturn("mockRefreshToken");
    given(jwtTokenProvider.getRefreshTokenValidTime()).willReturn(1209600000L);
    given(jwtTokenProvider.getAccessTokenValidTime()).willReturn(1800000L);

    //when
    TokenDto response = authService.login(request, httpRequest);

    //then
    assertThat(response).isNotNull();
    assertThat(response.accessToken()).isEqualTo("mockAccessToken");
    assertThat(response.profileName()).isEqualTo("침착맨");
    then(authCommandService).should(times(1))
        .saveLoginHistory(auth, LoginStatus.SUCCESS, "User-Agent", "127.0.0.1");

    // Redis 검증
    verify(redisService).setValues(eq("RT:test@test.com"), eq("mockRefreshToken"),
        any(Duration.class));
    verify(authCommandService).saveLoginHistory(eq(auth), eq(LoginStatus.SUCCESS), anyString(),
        anyString());

  }

  @Test
  @DisplayName("로그인 - 비밀번호 불일치 시 예외 발생")
  public void loginFailPasswordMismatch() {
    //given
    LoginRequest request = new LoginRequest(email, "wrongPassword");
    Auth auth = Auth.builder().role(Role.USER).build();
    AuthLocal authLocal = AuthLocal.builder()
        .email(email).passwordHash("encoded").auth(auth).build();

    given(authCommandService.getAuthLocalByEmail(email)).willReturn(authLocal);
    given(passwordEncoder.matches(request.password(), authLocal.getPasswordHash())).willReturn(
        false);

    //when & then
    assertThatThrownBy(() -> authService.login(request, httpRequest))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining(ErrorCode.AUTH_MISMATCH_PASSWORD.getMessage());
    then(authCommandService).should(times(1))
        .saveLoginHistory(auth, LoginStatus.FAILURE, "User-Agent", "127.0.0.1");
    verify(authCommandService).saveLoginHistory(eq(auth), eq(LoginStatus.FAILURE), anyString(),
        anyString());
  }

  @Test
  @DisplayName("로그아웃 - 명시적 로그아웃 및 블랙리스트 등록")
  public void logoutSuccess() {
    //given
    String accessToken = "mockAccessToken";
    given(redisService.hasKey("RT:" + email)).willReturn(true);
    given(jwtTokenProvider.getExpiration(accessToken)).willReturn(1800000L);
    //when
    authService.invalidateToken(email, accessToken);

    //then
    verify(redisService).deleteValues("RT:" + email);
    verify(redisService).setValues(eq("BlackList:" + accessToken), eq("logout"),
        any(Duration.class));
  }

  @Test
  @DisplayName("토큰 재발급 - 유요한 RT로 AT 갱신")
  public void reissueTokenSuccess() {
    //given
    String oldRefreshToken = "oldRefreshToken";
    String redisKey = "RT:" + email;

    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", 1L);
    AuthLocal authLocal = AuthLocal.builder().email(email).passwordHash("Qwer1234").auth(auth)
        .build();
    TokenDto newToken = TokenDto.of("newAccessToken", "newRefreshToken", 1000L, 9999L, auth,
        "프로필명");
    AuthUserInfoDto userInfo = AuthUserInfoDto.of("침착맨", 1L, null);

    given(jwtTokenProvider.validateToken(oldRefreshToken)).willReturn(true);
    given(jwtTokenProvider.getEmailFromToken(oldRefreshToken)).willReturn(email);
    given(redisService.getValues(redisKey)).willReturn(oldRefreshToken);
    given(authCommandService.getAuthLocalByEmail(email)).willReturn(authLocal);
    given(authCommandService.getAuthUserInfo(auth)).willReturn(userInfo);
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

  @Test
  @DisplayName("토큰 재발급 - 유효기간이 만료된 RT로 재발급 시도 실패")
  public void reissueFailedByExpiredRefreshToken() {
    // given
    String expiredRefreshToken = "expired.refresh.token";
    given(jwtTokenProvider.validateToken(expiredRefreshToken)).willReturn(false);

    // when & then
    BusinessException exception = assertThrows(BusinessException.class, () ->
        authService.reissueToken(expiredRefreshToken)
    );

    assertEquals(ErrorCode.AUTH_INVALID_TOKEN, exception.getErrorCode());
    verify(redisService, never()).getValues(anyString());
  }

  @Test
  @DisplayName("토큰 재발급 - 토큰 탈취 감지 시 강제 로그아웃")
  void reissue_ExceptionTokenTheftSuspected() {
    // given
    String refreshToken = "stolen.refresh.token";

    given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
    given(jwtTokenProvider.getEmailFromToken(refreshToken)).willReturn(email);

    given(redisService.getValues("RT:" + email)).willReturn("different.refresh.token");

    // when & then
    BusinessException exception = assertThrows(BusinessException.class,
        () -> authService.reissueToken(refreshToken));
    assertEquals(ErrorCode.AUTH_SUSPECT_THEFT_TOKEN, exception.getErrorCode());

    verify(redisService, times(1)).deleteValues("RT:" + email);
  }

  @Test
  @DisplayName("로그인 - X-Forwarded-For 헤더에 다수의 IP가 존재할 경우 첫 번째 IP를 정확히 추출한다")
  void loginExtractsFirstIpFromXForwardedFor() {
    // given
    ArgumentCaptor<String> ipAddressCaptor = ArgumentCaptor.forClass(String.class);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", "192.168.0.1, 10.0.0.1, 172.16.0.1");
    request.setRemoteAddr("127.0.0.1");

    LoginRequest loginRequest = new LoginRequest("test@test.com", "password123");
    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", 1L);

    AuthLocal authLocal = AuthLocal.builder().email("test@test.com").passwordHash("hashed")
        .auth(auth).build();

    given(authCommandService.getAuthLocalByEmail(loginRequest.email())).willReturn(authLocal);
    given(passwordEncoder.matches(loginRequest.password(), authLocal.getPasswordHash())).willReturn(
        true);
    given(authCommandService.getAuthUserInfo(auth)).willReturn(
        new AuthUserInfoDto("침착맨", 1L, null));
    given(jwtTokenProvider.createAccessToken(any(), any(), any(), any(), any())).willReturn(
        "access.token");
    given(jwtTokenProvider.createRefreshToken(any())).willReturn("refresh.token");
    given(jwtTokenProvider.getRefreshTokenValidTime()).willReturn(86400000L);
    given(jwtTokenProvider.getAccessTokenValidTime()).willReturn(3600000L);

    // when
    authService.login(loginRequest, request);

    // then
    verify(authCommandService).saveLoginHistory(
        eq(auth),
        eq(LoginStatus.SUCCESS),
        any(),
        ipAddressCaptor.capture()
    );
    assertThat(ipAddressCaptor.getValue()).isEqualTo("192.168.0.1");
  }

  @Test
  @DisplayName("토큰 무효화 - 이미 만료되었거나 Redis에 없는 토큰이라도 남은 시간만큼 블랙리스트에 정상 등록된다")
  void invalidateTokenSuccessfullyAddsToBlacklist() {
    // given
    String email = "test@test.com";
    String accessToken = "valid.access.token";
    long expirationTime = 1800000L; // 30분

    given(redisService.hasKey("RT:" + email)).willReturn(false);
    given(jwtTokenProvider.getExpiration(accessToken)).willReturn(expirationTime);

    // when
    authService.invalidateToken(email, accessToken);

    // then
    verify(redisService, never()).deleteValues(anyString());
    verify(redisService, times(1)).setValues(
        eq("BlackList:" + accessToken),
        eq("logout"),
        eq(Duration.ofMillis(expirationTime))
    );
  }

  @Test
  @DisplayName("IP 추출 엣지 케이스 - X-Forwarded-For 헤더가 null, 빈 문자열, 또는 unknown일 경우 RemoteAddr을 반환한다")
  public void extractIpAddressEdgeCasesReturnsRemoteAddress() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.99");

    // 1. null인 경우
    // 헤더를 추가하지 않음
    String resultNull = ReflectionTestUtils.invokeMethod(authService, "extractIpAddress", request);
    assertThat(resultNull).isEqualTo("10.0.0.99");

    // 2. 빈 문자열인 경우
    request.addHeader("X-Forwarded-For", "");
    String resultEmpty = ReflectionTestUtils.invokeMethod(authService, "extractIpAddress", request);
    assertThat(resultEmpty).isEqualTo("10.0.0.99");

    // 3. unknown인 경우 (대소문자 무시)
    request.removeHeader("X-Forwarded-For");
    request.addHeader("X-Forwarded-For", "UnKnOwN");
    String resultUnknown = ReflectionTestUtils.invokeMethod(authService, "extractIpAddress",
        request);
    assertThat(resultUnknown).isEqualTo("10.0.0.99");
  }

  @Test
  @DisplayName("로그아웃 시 Redis에 RT 키가 존재하면 삭제 로직을 정상 수행한다 (Branch 통과)")
  public void invalidateTokenHasKeyDeletesFromRedis() {
    // given
    String email = "test@test.com";
    String accessToken = "valid.token";
    given(redisService.hasKey("RT:" + email)).willReturn(true);
    given(jwtTokenProvider.getExpiration(accessToken)).willReturn(1000L);

    // when
    authService.invalidateToken(email, accessToken);

    // then
    verify(redisService, times(1)).deleteValues("RT:" + email);
  }
}