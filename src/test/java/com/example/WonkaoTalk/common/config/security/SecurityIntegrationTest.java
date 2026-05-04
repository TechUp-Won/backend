package com.example.WonkaoTalk.common.config.security;

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import java.security.Key;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private JwtTokenProvider jwtTokenProvider;
  @Value("${jwt.secret}")
  private String secretKey;

  @Test
  @DisplayName("유효한 JWT 토큰으로 인증을 시도하면 정상적으로 접근이 가능하다.(200)")
  public void requestWithValidTokenSuccess() throws Exception {
    //given
    String validToken = jwtTokenProvider.createAccessToken("test@test.com", 1L, "USER");

    //when & then
    mockMvc.perform(get("/api/v1/health/user")
            .header("Authorization", "Bearer " + validToken))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("토큰 없이 또는 유효하지 않은 형태의 토큰으로 접근하면 예외 발생(401)")
  public void requestWithInvalidTokenFailure() throws Exception {
    //given
    String invalidToken = "Bearer this.is.invalidToken";

    //when & then
    mockMvc.perform(get("/api/v1/health/user")
            .header("Authorization", invalidToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value(ErrorCode.AUTH_INVALID_TOKEN.getCode()));
  }

  @Test
  @DisplayName("유효기간이 만료된 토큰으로 인증을 시도하면 예외 발생 (401)")
  public void requestWithExpiredTokenFailure() throws Exception {
    //given
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    Key key = hmacShaKeyFor(keyBytes);
    Date pastExpiration = new Date(System.currentTimeMillis() - 60000);

    // 유효기간이 만료된 토큰 생성
    String expiredToken = Jwts.builder()
        .setSubject("test@test.com")
        .claim("authId", 1L)
        .claim("ROLE", "USER")
        .setExpiration(pastExpiration)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();

    //when & then
    mockMvc.perform(get("/appi/v1/health/user")
            .header("Authorization", "Bearer " + expiredToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value(ErrorCode.AUTH_EXPIRED_TOKEN.getCode()));
  }

  @Test
  @DisplayName("권한에 맞지 않은 접근을 시도하면 예외 발생(403)")
  public void requestWithRoleMismatchFailure() throws Exception {
    //given
    String userToken = jwtTokenProvider.createAccessToken("user@user.com", 1L, "USER");

    //when & then
    mockMvc.perform(get("/api/v1/health/seller")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("공개 엔드포인트는 토큰 없이도 접근 가능하다.")
  void publicAccess_Success() throws Exception {
    mockMvc.perform(get("/api/v1/health/public"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("USER 권한을 가진 사용자는 USER 엔드포인트에 접근 가능하다.")
  void userAccess_Success() throws Exception {
    String token = jwtTokenProvider.createAccessToken("user@test.com", 1L, "USER");
    mockMvc.perform(get("/api/v1/health/user")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("SELLER 권한을 가진 사용자는 SELLER 엔드포인트에 접근 가능하다.")
  void sellerAccess_Success() throws Exception {
    String token = jwtTokenProvider.createAccessToken("seller@test.com", 2L, "SELLER");
    mockMvc.perform(get("/api/v1/health/seller")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }
}