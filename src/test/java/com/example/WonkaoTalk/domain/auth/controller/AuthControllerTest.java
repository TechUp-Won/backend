package com.example.WonkaoTalk.domain.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.WonkaoTalk.common.config.JacksonConfig;
import com.example.WonkaoTalk.common.config.security.jwt.JwtExceptionFilter;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckResponse;
import com.example.WonkaoTalk.domain.auth.dto.LoginRequest;
import com.example.WonkaoTalk.domain.auth.dto.TokenDto;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
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

  @Test
  @DisplayName("이메일 중복 검사 - 미가입 이메일 검사 성공")
  void checkEmailSuccessTrue() throws Exception {
    //given
    EmailCheckRequest request = new EmailCheckRequest("test@test.com");
    EmailCheckResponse response = new EmailCheckResponse(true);
    given(authService.validateEmail(request)).willReturn(response);

    //when & then
    mockMvc.perform(get("/api/v1/auth/email-check")
            .param("email", "test@test.com"))
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
    mockMvc.perform(get("/api/v1/auth/email-check")
            .param("email", "test@test.com"))
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
    given(authService.login(any(LoginRequest.class), any(HttpServletRequest.class))).willReturn(
        mockToken);

    //when & then
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").exists());
  }

  @Test
  @DisplayName("로그아웃 - 명시적 로그아웃 및 블랙리스트 등록")
  void logoutSuccess() throws Exception {
    // given
    String validAccessToken = "Bearer valid.access.token";

    // when & then
    mockMvc.perform(post("/api/v1/auth/logout")
            .header("Authorization", validAccessToken))
        .andExpect(status().isOk());
  }

//  @Test
//  @DisplayName("로그아웃 - 이미 만료된 토큰으로 로그아웃 시도 실패")
//  public void logoutFailed() throws Exception {
//    //given
//    String expiredAccessToken = "Bearer expired.access.token";
//    //when & then
//    mockMvc.perform(post("/api/v1/auth/logout")
//            .header("Authorization", expiredAccessToken))
//        .andExpect(jsonPath("$.status").value("ERROR"))
//        .andExpect(jsonPath("$.error").value("AUTH-INVALID-TOKEN"));
//  }

  @Test
  @DisplayName("로그아웃 - 인증되지 않은 사용자의 로그아웃 시도 실패")
  void logout_Fail_UnauthenticatedUser() throws Exception {
    // given
    // Authorization 헤더가 없는 상태의 요청 (인증되지 않은 사용자)

    // when & then
    mockMvc.perform(post("/api/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON))
        // Spring Security의 AuthenticationEntryPoint에 의해 401 반환 검증
        .andExpect(status().isUnauthorized());
  }
}