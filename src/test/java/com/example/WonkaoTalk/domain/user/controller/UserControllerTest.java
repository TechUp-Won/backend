package com.example.WonkaoTalk.domain.user.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.WonkaoTalk.application.facade.AccountWithdraw;
import com.example.WonkaoTalk.common.config.security.SecurityConfig;
import com.example.WonkaoTalk.common.config.security.jwt.JwtAuthenticationFilter;
import com.example.WonkaoTalk.common.config.security.jwt.JwtExceptionFilter;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.user.dto.UserSearchRequest;
import com.example.WonkaoTalk.domain.user.dto.UserSearchResponse;
import com.example.WonkaoTalk.domain.user.service.UserService;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(
    controllers = UserController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                SecurityConfig.class,
                JwtAuthenticationFilter.class,
                JwtExceptionFilter.class
            }
        )
    }
)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private AccountWithdraw accountWithdraw;

  @MockitoBean(name = "bcryptExecutor")
  private Executor bcryptExecutor;

  private CustomUserDetails userDetails;

  @BeforeEach
  void setUp() {
    List<GrantedAuthority> authorities = Collections.singletonList(
        new SimpleGrantedAuthority("ROLE_" + "USER")
    );

    userDetails = CustomUserDetails.customBuilder()
        .email("test@test.com")
        .authId(1L)
        .userId(100L)
        .sellerId(null)
        .authorities(authorities)
        .build();
  }

  @Test
  @DisplayName("회원 탈퇴 API 성공")
  public void withdrawSuccess() throws Exception {
    // given
    String token = "valid.jwt.token";

    // when & then
    mockMvc.perform(delete("/api/v1/users/withdraw")
            .header("Authorization", "Bearer " + token)
            .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."));

    verify(accountWithdraw).withdrawUser(1L, "test@test.com", token);
  }

  @Test
  @DisplayName("회원 탈퇴 API 실패 - Authorization 헤더 누락/형식 오류")
  public void withdrawFailInvalidHeader() throws Exception {
    // Bearer 접두사가 없는 잘못된 토큰 형식
    mockMvc.perform(delete("/api/v1/users/withdraw")
            .header("Authorization", "InvalidTokenFormat")
            .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
            .with(csrf()))
        .andExpect(status().isUnauthorized()) // 401 상태 코드로 검증 변경
        .andExpect(jsonPath("$.error").value("AUTH-INVALID-TOKEN"));
  }

  @Test
  @DisplayName("전화번호로 사용자 검색 API 성공")
  public void searchUserByPhoneSuccess() throws Exception {
    // given
    UserSearchRequest request = new UserSearchRequest("010-1234-5678");
    UserSearchResponse response = new UserSearchResponse(2L, "010-1234-5678", "침착맨", "image.png");

    given(userService.findUserByPhone(eq(100L), eq("010-1234-5678"))).willReturn(response);

    // when & then
    mockMvc.perform(post("/api/v1/users/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value(2L))
        .andExpect(jsonPath("$.data.nickname").value("침착맨"));
  }

}