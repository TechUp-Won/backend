package com.example.WonkaoTalk.domain.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.application.facade.AccountWithdraw;
import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.oauth.GoogleRevocationProvider;
import com.example.WonkaoTalk.common.oauth.NaverRevocationProvider;
import com.example.WonkaoTalk.common.oauth.OAuthRevocationClient;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthSocialRepo;
import com.example.WonkaoTalk.domain.auth.repo.OAuthRevocationFailureRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class UserIntegrationTest {

  @Autowired
  private AccountWithdraw accountWithdraw;
  @Autowired
  private AuthRepo authRepo;
  @Autowired
  private AuthSocialRepo authSocialRepo;
  @Autowired
  private UserRepo userRepo;
  @Autowired
  private OAuthRevocationFailureRepo failureRepo;
  @Autowired
  private JwtTokenProvider jwtTokenProvider;
  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private OAuthRevocationClient oAuthRevocationClient;
  @MockitoBean
  private GoogleRevocationProvider googleRevocationProvider;
  @MockitoBean
  private NaverRevocationProvider naverRevocationProvider;

  private Auth savedAuth;
  private User savedUser;
  private String validAccessToken;

  @BeforeEach
  void setUp() {
    Auth auth = Auth.builder().role(Role.USER).build();
    savedAuth = authRepo.save(auth);

    AuthSocial authSocial = AuthSocial.builder()
        .auth(savedAuth)
        .provider(AuthProvider.GOOGLE)
        .providerUserId("google_123")
        .providerRefreshToken("refresh_token_string")
        .email("test@test.com")
        .build();
    authSocialRepo.save(authSocial);

    User user = User.builder()
        .auth(savedAuth)
        .name("테스터")
        .nickname("테스터")
        .phone("010-0000-0000")
        .build();
    savedUser = userRepo.save(user);

    given(googleRevocationProvider.supports(AuthProvider.GOOGLE)).willReturn(true);

    validAccessToken = jwtTokenProvider.createAccessToken("test@example.com", savedAuth.getId(),
        user.getId(), null, "USER");
  }

  @AfterEach
  void tearDown() {
    // @Transactional이 없으므로 수동 데이터 정리
    jdbcTemplate.execute(
        "TRUNCATE TABLE users, auth_socials, auth_locals, auths, oauth_revocation_failures RESTART IDENTITY CASCADE");
  }

  @Test
  @DisplayName("시나리오 1: [정상] 회원 탈퇴 시 트랜잭션이 묶이며 Soft Delete가 정상적으로 수행된다.")
  void withdrawUser_Success_SoftDelete() throws Exception {
    // given
    willDoNothing().given(googleRevocationProvider).revoke(anyString(), anyString());

    // when
    accountWithdraw.withdrawUser(savedAuth.getId(), "test@test.com", validAccessToken);

    // then
    assertThat(authRepo.findById(savedAuth.getId())).isEmpty();
    assertThat(userRepo.findByAuthId(savedAuth.getId())).isEmpty();

    java.sql.Timestamp authDeletedAt = jdbcTemplate.queryForObject(
        "SELECT deleted_at FROM auths WHERE id = ?",
        java.sql.Timestamp.class,
        savedAuth.getId()
    );
    assertThat(authDeletedAt).isNotNull(); // Soft Delete 마킹 확인

    java.sql.Timestamp userDeletedAt = jdbcTemplate.queryForObject(
        "SELECT deleted_at FROM users WHERE auth_id = ?",
        java.sql.Timestamp.class,
        savedAuth.getId()
    );
    assertThat(userDeletedAt).isNotNull(); // Soft Delete 마킹 확인

    assertThat(failureRepo.findAll()).isEmpty();
    verify(googleRevocationProvider, timeout(2000).times(1)).revoke(anyString(), anyString());
  }

  @Test
  @DisplayName("시나리오 2: [예외] 외부 OAuth 해지 실패 시 예외를 잡아내어 메인 로직은 커밋하고, 실패 이력을 기록한다.")
  void withdrawUser_OAuthRevocationFail_CompletesSoftDeleteAndLogsFailure() throws Exception {
    // given
    willThrow(new RuntimeException("External Provider Timeout"))
        .given(googleRevocationProvider).revoke(anyString(), anyString());

    // when
    accountWithdraw.withdrawUser(savedAuth.getId(), "test@test.com", validAccessToken);

    // then
    assertThat(authRepo.findById(savedAuth.getId())).isEmpty();
    assertThat(userRepo.findByAuthId(savedAuth.getId())).isEmpty();

    java.sql.Timestamp authDeletedAt = jdbcTemplate.queryForObject(
        "SELECT deleted_at FROM auths WHERE id = ?",
        java.sql.Timestamp.class,
        savedAuth.getId()
    );
    assertThat(authDeletedAt).isNotNull();

    java.sql.Timestamp userDeletedAt = jdbcTemplate.queryForObject(
        "SELECT deleted_at FROM users WHERE auth_id = ?",
        java.sql.Timestamp.class,
        savedAuth.getId()
    );
    assertThat(userDeletedAt).isNotNull();

    long failureCount = failureRepo.count();
    assertThat(failureCount).isEqualTo(1L);
  }
}
