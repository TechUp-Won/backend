package com.example.WonkaoTalk.domain.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.WonkaoTalk.common.integration.BaseIntegrationTest;
import com.example.WonkaoTalk.domain.auth.dto.LoginRequest;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.entity.LoginHistory;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.repo.AuthLocalRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.auth.repo.LoginHistoryRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthIntegrationTest extends BaseIntegrationTest {

  private final String testEmail = "integration@test.com";
  private final String testPassword = "ValidPassword123!";
  @Autowired
  private AuthRepo authRepo;
  @Autowired
  private AuthLocalRepo authLocalRepo;
  @Autowired
  private UserRepo userRepo;
  @Autowired
  private LoginHistoryRepo loginHistoryRepo;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    Auth auth = Auth.builder().role(Role.USER).build();
    authRepo.save(auth);
    AuthLocal authLocal = AuthLocal.builder()
        .email(testEmail)
        .passwordHash(passwordEncoder.encode(testPassword))
        .auth(auth)
        .build();
    authLocalRepo.save(authLocal);
    User user = User.builder()
        .auth(auth)
        .nickname("TestUser")
        .name("TestUser")
        .phone("010-0000-0000")
        .build();
    userRepo.save(user);
  }

  @Test
  @DisplayName("시나리오 1: 정상 로그인 시 토큰 발급 및 접속 이력 저장 확인")
  void login_Success_ReturnsTokenAndSavesHistory() throws Exception {
    // given
    long initialHistoryCount = loginHistoryRepo.count();
    LoginRequest request = new LoginRequest(testEmail, testPassword);

    // when & then
    // 로그인은 별도 스레드풀(bcryptExecutor)에서 비동기로 처리되므로 비동기 디스패치를 거쳐 결과를 확인한다.
    MvcResult mvcResult = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(request().asyncStarted())
        .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tokenInfo.accessToken").exists())
        .andExpect(jsonPath("$.data.tokenInfo.grantType").value("Bearer"));

    long finalHistoryCount = loginHistoryRepo.count();
    assertThat(finalHistoryCount).isEqualTo(initialHistoryCount + 1);

    LoginHistory savedHistory = loginHistoryRepo.findAll().get((int) finalHistoryCount - 1);
    assertThat(savedHistory.getAuth().getId()).isNotNull();
  }

  @Test
  @DisplayName("시나리오 2-A: 조작된 토큰으로 API 접근 시 Security 필터에서 차단 및 401 반환")
  void access_WithManipulatedToken_ReturnsUnauthorized() throws Exception {
    // given
    String manipulatedToken = "Bearer eyJhbGciOiJIUzI1NiJ9.ManipulatedPayload.InvalidSignature";

    // when & then
    mockMvc.perform(get("/api/v1/users/me")
            .header("Authorization", manipulatedToken))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.error").value("AUTH-INVALID-TOKEN"));
  }

  @Test
  @DisplayName("시나리오 2-B: 만료된 토큰으로 API 접근 시 Security 필터에서 차단 및 401 반환")
  void access_WithExpiredToken_ReturnsUnauthorized() throws Exception {
    // given
    String expiredToken = "Bearer " + createExpiredTokenForTest(testEmail);

    // when & then
    mockMvc.perform(get("/api/v1/users/me")
            .header("Authorization", expiredToken))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.error").value("AUTH-EXPIRED-TOKEN"));
  }

  private String createExpiredTokenForTest(String email) {
    Date now = new Date();
    // 현재 시간으로부터 1시간 전으로 만료 시간을 설정 (과거)
    Date pastExpiration = new Date(now.getTime() - (1000 * 60 * 60));

    byte[] keyBytes = Decoders.BASE64.decode(secretKey);

    return Jwts.builder()
        .setSubject(email)
        .setIssuedAt(now)
        .setExpiration(pastExpiration)
        .signWith(Keys.hmacShaKeyFor(keyBytes), SignatureAlgorithm.HS256)
        .compact();
  }
}
