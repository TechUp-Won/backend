package com.example.WonkaoTalk.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.WonkaoTalk.common.config.JacksonConfig;
import com.example.WonkaoTalk.common.config.security.OAuth2SuccessHandler;
import com.example.WonkaoTalk.common.config.security.SecurityConfig;
import com.example.WonkaoTalk.common.config.security.jwt.JwtExceptionFilter;
import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.common.redis.RedisService;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckResponse;
import com.example.WonkaoTalk.domain.auth.dto.LoginRequest;
import com.example.WonkaoTalk.domain.auth.dto.TokenDto;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import com.example.WonkaoTalk.domain.auth.service.OAuth2UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, SecurityConfig.class})
class AuthControllerTest {

  private final Auth auth = Auth.builder().build();
  private final TokenDto mockToken = TokenDto.of("accessToken", "refreshToken", 1000L, 5000L, auth,
      "침착맨");
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private AuthService authService;
  @MockitoBean
  private JwtExceptionFilter jwtExceptionFilter;
  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;
  @MockitoBean
  private RedisService redisService;
  @MockitoBean
  private OAuth2UserService oAuth2UserService;
  @MockitoBean
  private OAuth2SuccessHandler oAuth2SuccessHandler;

  @Test
  @DisplayName("이메일 중복 검사 - 미가입 이메일 검사 성공")
  void checkEmailSuccessTrue() throws Exception {
    //given
    EmailCheckRequest request = new EmailCheckRequest("test@test.com");
    EmailCheckResponse response = new EmailCheckResponse(true);
    given(authService.validateEmail(request)).willReturn(response);

    //when & then
    mockMvc.perform(post("/api/v1/auth/check-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.isValid").value(true));
  }

  @Test
  @DisplayName("이메일 중복 검사 - 기가입 이메일 검사 성공")
  void checkEmailSuccessDuplicated() throws Exception {
    //given
    EmailCheckRequest request = new EmailCheckRequest("test@test.com");
    EmailCheckResponse response = new EmailCheckResponse(false);
    given(authService.validateEmail(request)).willReturn(response);

    //when & then
    mockMvc.perform(post("/api/v1/auth/check-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.isValid").value(false));
  }

  @Test
  @DisplayName("이메일 중복 검사 - 정규식 위반 이메일 차단")
  public void checkEmailFailedByInvalidEmail() throws Exception {
    //given
    EmailCheckRequest request = new EmailCheckRequest("test@test");

    //when & then
    mockMvc.perform(post("/api/v1/auth/check-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.error").value("SYS-INVALID-INPUT"));
  }

  @Test
  @DisplayName("로그인 - 유효한 자격증명으로 토큰 발급")
  void loginSuccess() throws Exception {
    //given
    LoginRequest request = new LoginRequest("test@example.com", "Qwer1234");
    given(authService.login(any(LoginRequest.class), any(), any())).willReturn(
        CompletableFuture.completedFuture(mockToken));

    //when & then
    MvcResult mvcResult = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tokenInfo.accessToken").exists());
  }

  @Test
  @DisplayName("로그인 - X-Forwarded-For 헤더에 다수의 IP가 존재할 경우 첫 번째 IP를 서비스에 전달한다")
  void loginExtractsFirstIpFromXForwardedFor() throws Exception {
    //given
    LoginRequest request = new LoginRequest("test@example.com", "Qwer1234");
    given(authService.login(any(LoginRequest.class), any(), any())).willReturn(
        CompletableFuture.completedFuture(mockToken));

    //when
    MvcResult mvcResult = mockMvc.perform(post("/api/v1/auth/login")
            .header("X-Forwarded-For", "192.168.0.1, 10.0.0.1, 172.16.0.1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(request().asyncStarted())
        .andReturn();
    mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk());

    //then
    ArgumentCaptor<String> ipAddressCaptor = ArgumentCaptor.forClass(String.class);
    verify(authService).login(any(LoginRequest.class), any(), ipAddressCaptor.capture());
    assertThat(ipAddressCaptor.getValue()).isEqualTo("192.168.0.1");
  }

  @Test
  @DisplayName("IP 추출 엣지 케이스 - X-Forwarded-For 헤더가 null, 빈 문자열, 또는 unknown일 경우 RemoteAddr을 반환한다")
  void extractIpAddressEdgeCasesReturnsRemoteAddress() {
    // given
    AuthController controller = new AuthController(authService);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.99");

    // 1. null인 경우 (헤더를 추가하지 않음)
    String resultNull = ReflectionTestUtils.invokeMethod(controller, "extractIpAddress", request);
    assertThat(resultNull).isEqualTo("10.0.0.99");

    // 2. 빈 문자열인 경우
    request.addHeader("X-Forwarded-For", "");
    String resultEmpty = ReflectionTestUtils.invokeMethod(controller, "extractIpAddress", request);
    assertThat(resultEmpty).isEqualTo("10.0.0.99");

    // 3. unknown인 경우 (대소문자 무시)
    request.removeHeader("X-Forwarded-For");
    request.addHeader("X-Forwarded-For", "UnKnOwN");
    String resultUnknown = ReflectionTestUtils.invokeMethod(controller, "extractIpAddress",
        request);
    assertThat(resultUnknown).isEqualTo("10.0.0.99");
  }

  @Test
  @DisplayName("로그아웃 - 명시적 로그아웃 및 블랙리스트 등록")
  @WithMockUser(username = "test@test.com")
  void logoutSuccess() throws Exception {
    // given
    String validAccessToken = "Bearer valid.access.token";

    // when & then
    mockMvc.perform(post("/api/v1/auth/logout")
            .header("Authorization", validAccessToken))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("로그아웃 - 이미 만료된 토큰으로 로그아웃 시도 실패")
  @WithMockUser(username = "test@test.com")
  public void logoutFailed() throws Exception {
    //given
    String expiredAccessToken = "Bearer expired.access.token";
    String parsedToken = "expired.access.token";

    willThrow(new BusinessException(ErrorCode.AUTH_INVALID_TOKEN)).given(authService)
        .invalidateToken(eq("test@test.com"), eq(parsedToken));
    //when & then
    mockMvc.perform(post("/api/v1/auth/logout")
            .header("Authorization", expiredAccessToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.error").value("AUTH-INVALID-TOKEN"));
  }

  @Test
  @DisplayName("로그아웃 - 인증되지 않은 사용자의 로그아웃 시도 실패")
  void logoutFailUnauthenticatedUser() throws Exception {
    // given
    // Authorization 헤더가 없는 상태의 요청 (인증되지 않은 사용자)

    // when & then
    mockMvc.perform(post("/api/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.error").value("AUTH-INVALID-TOKEN"));
  }

  @Test
  @DisplayName("토큰 재발급 - 쿠키에 refresh-token이 누락된 경우 AUTH_MISSING_TOKEN 예외 응답을 반환한다")
  void reissueTokenMissingCookieThrowsException() throws Exception {
    // given & when & then
    mockMvc.perform(post("/api/v1/auth/reissue")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.error").value("AUTH-MISSING-TOKEN"));
  }

  @Test
  @DisplayName("토큰 재발급 - 유효한 리프레시 토큰을 쿠키로 전달하면 새로운 토큰 정보와 Set-Cookie 헤더를 반환한다")
  void reissueTokenSuccess() throws Exception {
    // given
    String validRefreshToken = "valid.refresh.token";
    TokenDto mockTokenDto = TokenDto.of(
        "new.access.token",
        "new.refresh.token",
        3600000L,
        86400000L,
        Auth.builder().build(),
        "침착맨"
    );

    given(authService.reissueToken(validRefreshToken)).willReturn(mockTokenDto);

    // when & then
    mockMvc.perform(post("/api/v1/auth/reissue")
            .cookie(new jakarta.servlet.http.Cookie("refresh-token", validRefreshToken)))
        .andExpect(status().isOk())
        .andExpect(header().exists(org.springframework.http.HttpHeaders.SET_COOKIE))
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.tokenInfo.accessToken").value("new.access.token"))
        .andExpect(jsonPath("$.data.userInfo.profileName").value("침착맨"));
  }

  @Test
  @DisplayName("로그인 유효성 검사 실패 - 올바르지 않은 이메일 형식 요청 시 컨트롤러 진입 전 400 에러로 차단된다")
  void loginInvalidEmailValidationFailed() throws Exception {
    // given
    LoginRequest invalidRequest = new LoginRequest("invalid-email-format", "password123");

    // when & then
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.message").value(containsString("올바른 이메일 형식이 아닙니다")));
  }

  @Test
  @DisplayName("로그인 실패 - 서비스에서 비밀번호 불일치 비즈니스 예외가 발생하면 전역 예외 처리기가 에러 포맷으로 변환한다")
  void loginPasswordMismatchReturnsErrorResponse() throws Exception {
    // given
    LoginRequest request = new LoginRequest("test@test.com", "wrong_password");
    given(authService.login(any(LoginRequest.class), any(), any()))
        .willThrow(new BusinessException(ErrorCode.AUTH_MISMATCH_PASSWORD));

    // when & then
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest()) //
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.error").value("AUTH-MISMATCH-PASSWORD"));
  }
}
