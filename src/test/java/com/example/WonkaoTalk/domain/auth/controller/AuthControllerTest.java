package com.example.WonkaoTalk.domain.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class)
// Spring Security 권한을 우회
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AuthService authService;

  @Test
  @DisplayName("올바르지 않은 이메일 형식으로 중복 검사 실패")
  public void CheckEmailFailedByInvalidEmail() throws Exception {
    //given
    EmailCheckRequest request = new EmailCheckRequest("test@test"); // 도메인 누락

    //when & then
    mockMvc.perform(post("/api/v1/auth/check-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.error").value("AUTH-INVALID-EMAIL")); // ErrorCode 검증
  }

}