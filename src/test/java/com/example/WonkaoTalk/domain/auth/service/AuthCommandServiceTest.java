package com.example.WonkaoTalk.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.dto.AuthIntegrationResultDto;
import com.example.WonkaoTalk.domain.auth.dto.AuthUserInfoDto;
import com.example.WonkaoTalk.domain.auth.dto.SocialLoginDto;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.entity.AuthSocial;
import com.example.WonkaoTalk.domain.auth.entity.LoginHistory;
import com.example.WonkaoTalk.domain.auth.enums.AuthProvider;
import com.example.WonkaoTalk.domain.auth.enums.LoginStatus;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.repo.AuthLocalRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthSocialRepo;
import com.example.WonkaoTalk.domain.auth.repo.LoginHistoryRepo;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

  private final Long authId = 1L;
  @Mock
  LoginHistoryRepo loginHistoryRepo;
  @InjectMocks
  private AuthCommandService authCommandService;
  @Mock
  private AuthRepo authRepo;
  @Mock
  private AuthLocalRepo authLocalRepo;
  @Mock
  private AuthSocialRepo authSocialRepo;
  @Mock
  private UserRepo userRepo;
  @Mock
  private SellerRepo sellerRepo;

  @Test
  @DisplayName("일반/소셜 통합 계정에서 유저 탈퇴 시, 활성화된 판매자가 있다면 Role만 SELLER로 부분 탈퇴된다.")
  public void handleUserWithdrawPartialWithdrawRoleDowngraded() {
    // given
    Auth auth = Auth.builder().role(Role.USER_SELLER).build();
    ReflectionTestUtils.setField(auth, "id", authId);

    given(authRepo.findById(authId)).willReturn(Optional.of(auth));

    // when
    authCommandService.handleUserWithdraw(authId, true);

    // then
    assertThat(auth.getRole()).isEqualTo(Role.SELLER);
    then(authLocalRepo).should(never()).findByAuth(auth);
    then(authSocialRepo).should(never()).findByAuth(auth);
  }

  @Test
  @DisplayName("유저 단일 계정 탈퇴 시, 활성화된 판매자가 없다면 연관된 인증 정보가 마스킹되고 완전 탈퇴(Soft Delete)된다.")
  public void handleUserWithdrawFullWithdrawAnonymizedAndDeleted() {
    // given
    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", authId);

    AuthLocal authLocal = AuthLocal.builder().email("test@test.com").passwordHash("hash").build();
    AuthSocial authSocial = AuthSocial.builder().providerUserId("google_12345").build();

    given(authRepo.findById(authId)).willReturn(Optional.of(auth));
    given(authLocalRepo.findByAuth(auth)).willReturn(Optional.of(authLocal));
    given(authSocialRepo.findByAuth(auth)).willReturn(List.of(authSocial));

    // when
    authCommandService.handleUserWithdraw(authId, false);

    // then
    assertThat(authLocal.getEmail()).isNotEqualTo("test@test.com");
    assertThat(authSocial.getProviderUserId()).isNotEqualTo("google_12345");

    then(authLocalRepo).should(times(1)).findByAuth(auth);
    then(authSocialRepo).should(times(1)).findByAuth(auth);
  }

  @Test
  @DisplayName("일반/소셜 통합 계정에서 판매자 탈퇴 시, 활성화된 유저가 있다면 Role만 USER로 부분 탈퇴된다.")
  public void handleSellerWithdrawPartialWithdrawRoleDowngraded() {
    // given
    Auth auth = Auth.builder().role(Role.USER_SELLER).build();
    ReflectionTestUtils.setField(auth, "id", authId);

    given(authRepo.findById(authId)).willReturn(Optional.of(auth));

    // when (활성화된 유저가 존재함 = true)
    authCommandService.handleSellerWithdraw(authId, true);

    // then (상태 검증)
    assertThat(auth.getRole()).isEqualTo(Role.USER);
    then(authLocalRepo).should(never()).findByAuth(auth);
  }

  @Test
  @DisplayName("존재하지 않는 계정 탈퇴 요청 시 BusinessException 예외가 발생한다.")
  public void handleWithdrawNotFoundThrowsException() {
    // given
    given(authRepo.findById(authId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authCommandService.handleUserWithdraw(authId, false))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_NOT_FOUND);
  }

  @Test
  @DisplayName("소셜 로그인 데이터 생성 - 일반 사용자(USER)인 경우 SellerId는 null이어야 한다.")
  public void generateSocialLoginDataUserRoleReturnsDtoWithoutSellerId() {
    // given
    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", 1L);
    AuthSocial authSocial = AuthSocial.builder().auth(auth).build();

    User user = User.builder().build();
    ReflectionTestUtils.setField(user, "id", 100L);

    given(authSocialRepo.findByProviderAndProviderUserIdWithAuth(AuthProvider.GOOGLE, "google_123"))
        .willReturn(Optional.of(authSocial));
    given(userRepo.findByAuth(auth)).willReturn(Optional.of(user));

    // when
    SocialLoginDto result = authCommandService.generateSocialLoginData("test@test.com",
        AuthProvider.GOOGLE, "google_123");

    // then
    assertThat(result.authId()).isEqualTo(1L);
    assertThat(result.userId()).isEqualTo(100L);
    assertThat(result.sellerId()).isNull();
    assertThat(result.role()).isEqualTo(Role.USER);
    verify(sellerRepo, never()).findByAuth(any());
  }

  @Test
  @DisplayName("소셜 로그인 데이터 생성 - 판매자(USER_SELLER)인 경우 UserId와 SellerId를 모두 반환한다.")
  public void generateSocialLoginDataSellerRoleReturnsDtoWithBothIds() {
    // given
    Auth auth = Auth.builder().role(Role.USER_SELLER).build();
    ReflectionTestUtils.setField(auth, "id", 1L);
    AuthSocial authSocial = AuthSocial.builder().auth(auth).build();

    User user = User.builder().build();
    ReflectionTestUtils.setField(user, "id", 100L);
    Seller seller = Seller.builder().build();
    ReflectionTestUtils.setField(seller, "id", 50L);

    given(authSocialRepo.findByProviderAndProviderUserIdWithAuth(AuthProvider.NAVER, "naver_123"))
        .willReturn(Optional.of(authSocial));
    given(userRepo.findByAuth(auth)).willReturn(Optional.of(user));
    given(sellerRepo.findByAuth(auth)).willReturn(Optional.of(seller));

    // when
    SocialLoginDto result = authCommandService.generateSocialLoginData("test@test.com",
        AuthProvider.NAVER, "naver_123");

    // then
    assertThat(result.userId()).isEqualTo(100L);
    assertThat(result.sellerId()).isEqualTo(50L);
    assertThat(result.role()).isEqualTo(Role.USER_SELLER);
  }

  @Test
  @DisplayName("소셜 로그인 데이터 생성 - 소셜 계정이 존재하지 않으면 AUTH_NOT_FOUND 예외가 발생한다.")
  public void generateSocialLoginDataNotFoundThrowsException() {
    // given
    given(authSocialRepo.findByProviderAndProviderUserIdWithAuth(any(), any()))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
        () -> authCommandService.generateSocialLoginData("test@test.com", AuthProvider.GOOGLE,
            "invalid_id"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.AUTH_NOT_FOUND);
  }

  @Test
  @DisplayName("로그인 이력 저장 - 파라미터가 LoginHistory 엔티티로 정상 변환되어 저장된다.")
  public void saveLoginHistoryConvertsAndSavesProperly() {
    // given
    Auth auth = Auth.builder().role(Role.USER).build();
    ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);

    // when
    authCommandService.saveLoginHistory(auth, LoginStatus.SUCCESS, "Mozilla/5.0", "192.168.0.1");

    // then
    verify(loginHistoryRepo, times(1)).save(captor.capture());
    LoginHistory savedHistory = captor.getValue();

    assertThat(savedHistory.getAuth()).isEqualTo(auth);
    assertThat(savedHistory.getStatus()).isEqualTo(LoginStatus.SUCCESS);
    assertThat(savedHistory.getUserAgent()).isEqualTo("Mozilla/5.0");
    assertThat(savedHistory.getIpAddress()).isEqualTo("192.168.0.1");
  }

  @Test
  @DisplayName("소셜 이메일 단건 조회 - 이메일 기반 첫 번째 소셜 계정을 정상적으로 반환한다.")
  public void getFirstAuthSocialByEmailReturnsFirstMatched() {
    // given
    String email = "test@test.com";
    AuthSocial social = AuthSocial.builder().email(email).build();
    given(authSocialRepo.findFirstByEmail(email)).willReturn(Optional.of(social));

    // when
    Optional<AuthSocial> result = authCommandService.getFirstAuthSocialByEmail(email);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo(email);
  }

  @Test
  @DisplayName("일반 계정 조회 - 이메일로 AuthLocal을 정상 조회한다")
  public void getAuthLocalByEmailSuccess() {
    // given
    String email = "test@test.com";
    AuthLocal authLocal = AuthLocal.builder().email(email).build();
    given(authLocalRepo.findByEmailWithAuth(email)).willReturn(Optional.of(authLocal));

    // when
    AuthLocal result = authCommandService.getAuthLocalByEmail(email);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo(email);
  }

  @Test
  @DisplayName("일반 계정 조회 - 이메일이 존재하지 않으면 예외가 발생한다")
  public void getAuthLocalByEmailNotFoundThrowsException() {
    // given
    given(authLocalRepo.findByEmailWithAuth(anyString())).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authCommandService.getAuthLocalByEmail("unknown@test.com"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.AUTH_INVALID_EMAIL);
  }

  @Test
  @DisplayName("인증 유저 정보 조회 - User 권한일 경우 UserRepo에서 프로필(닉네임)을 정상 반환한다")
  public void getAuthUserInfoUserRoleReturnsCorrectInfo() {
    // given
    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", 1L);

    User user = User.builder().build(); // name 대신 nickname 필드 사용 가정
    ReflectionTestUtils.setField(user, "id", 100L);
    ReflectionTestUtils.setField(user, "nickname", "침착맨");

    given(userRepo.findByAuth(auth)).willReturn(Optional.of(user));

    // when
    AuthUserInfoDto result = authCommandService.getAuthUserInfo(auth);

    // then
    assertThat(result.userId()).isEqualTo(100L);
    assertThat(result.profileName()).isEqualTo("침착맨");
    assertThat(result.sellerId()).isNull();
  }

  @Test
  @DisplayName("인증 유저 정보 조회 - 프로필 정보가 데이터베이스에 없으면 예외가 발생한다")
  public void getAuthUserInfoProfileNotFoundThrowsException() {
    // given
    Auth auth = Auth.builder().role(Role.USER).build();
    given(userRepo.findByAuth(auth)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authCommandService.getAuthUserInfo(auth))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("소셜 로그인 DTO 생성 - 관리자(ADMIN) 권한일 경우 sellerRepo 조회를 생략한다 (Branch 통과)")
  public void generateSocialLoginDataAdminRoleSkipsSellerRepo() {
    // given
    Auth auth = Auth.builder().role(Role.ADMIN).build();
    ReflectionTestUtils.setField(auth, "id", 1L);
    AuthSocial authSocial = AuthSocial.builder().auth(auth).build();
    User user = User.builder().build();
    ReflectionTestUtils.setField(user, "id", 99L);

    given(authSocialRepo.findByProviderAndProviderUserIdWithAuth(AuthProvider.GOOGLE, "123"))
        .willReturn(Optional.of(authSocial));
    given(userRepo.findByAuth(auth)).willReturn(Optional.of(user));

    // when
    SocialLoginDto dto = authCommandService.generateSocialLoginData("admin@test.com",
        AuthProvider.GOOGLE, "123");

    // then
    assertThat(dto.role()).isEqualTo(Role.ADMIN);
    assertThat(dto.sellerId()).isNull();
    verify(sellerRepo, never()).findByAuth(any());
  }

  @Test
  @DisplayName("완전 탈퇴 처리 - 연결된 AuthLocal과 AuthSocial이 없어도 안전하게 처리된다 (null-safe Branch)")
  public void processFullWithdrawEmptyRelationsDoesNotThrow() {
    // given
    Long authId = 1L;
    Auth auth = Auth.builder().role(Role.USER).build();
    ReflectionTestUtils.setField(auth, "id", authId);

    given(authRepo.findById(authId)).willReturn(Optional.of(auth));
    given(authLocalRepo.findByAuth(auth)).willReturn(Optional.empty()); // Local 없음
    given(authSocialRepo.findByAuth(auth)).willReturn(List.of()); // Social 없음

    // when
    authCommandService.handleUserWithdraw(authId, false);

    // then
    verify(authRepo, times(1)).findById(authId);
  }

  @Test
  @DisplayName("일반 계정 저장 - 이메일이 이미 존재하면 AUTH_DUPLICATE_EMAIL 예외가 발생한다")
  public void saveAuthLocalDuplicateEmailThrowsException() {
    // given
    Auth auth = Auth.builder().build();
    String email = "test@test.com";
    given(authLocalRepo.existsByEmail(email)).willReturn(true);

    // when & then
    assertThatThrownBy(() -> authCommandService.saveAuthLocal(auth, email, "password"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.AUTH_DUPLICATE_EMAIL);

    verify(authLocalRepo, never()).save(any());
  }

  @Test
  @DisplayName("일반 계정 저장 - 이메일 중복이 없으면 정상적으로 AuthLocal 엔티티가 저장된다")
  public void saveAuthLocalSuccess() {
    // given
    Auth auth = Auth.builder().build();
    String email = "test@test.com";
    given(authLocalRepo.existsByEmail(email)).willReturn(false);

    // when
    authCommandService.saveAuthLocal(auth, email, "password");

    // then
    verify(authLocalRepo, times(1)).save(any(AuthLocal.class));
  }

  @Test
  @DisplayName("인증 유저 정보 조회 - 순수 SELLER 권한일 때 SellerRepo에서만 정보를 가져온다")
  public void getAuthUserInfoOnlySellerRoleReturnsSellerInfo() {
    // given
    Auth auth = Auth.builder().role(Role.SELLER).build();
    Seller seller = Seller.builder().name("침착상회").build();
    ReflectionTestUtils.setField(seller, "id", 50L);

    given(sellerRepo.findByAuth(auth)).willReturn(Optional.of(seller));

    // when
    AuthUserInfoDto result = authCommandService.getAuthUserInfo(auth);

    // then
    assertThat(result.sellerId()).isEqualTo(50L);
    assertThat(result.profileName()).isEqualTo("침착상회");
    assertThat(result.userId()).isNull();
    verify(userRepo, never()).findByAuth(any());
  }

  @Test
  @DisplayName("인증 유저 정보 조회 - SELLER 권한이 포함되어 있으나 Seller 엔티티가 없으면 예외 발생")
  public void getAuthUserInfoSellerNotFoundThrowsException() {
    // given
    Auth auth = Auth.builder().role(Role.USER_SELLER).build();
    given(sellerRepo.findByAuth(auth)).willReturn(Optional.empty());
    User user = User.builder().build();
    ReflectionTestUtils.setField(user, "id", 100L);
    ReflectionTestUtils.setField(user, "nickname", "침착맨");
    given(userRepo.findByAuth(auth)).willReturn(Optional.of(user));

    // when & then
    assertThatThrownBy(() -> authCommandService.getAuthUserInfo(auth))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.SELLER_NOT_FOUND);
  }

  @Test
  @DisplayName("소셜 연동 처리 - 이미 소셜 이메일이 존재할 경우, 신규 Auth를 생성하지 않고 기존 Auth에 Local을 연동한다")
  public void linkOrCreateLocal_ExistingSocial_LinksToExistingAuth() {
    // given
    String email = "test@test.com";
    String password = "encodedPassword";

    Auth existingAuth = Auth.builder().role(Role.USER).build();
    AuthSocial existingSocial = AuthSocial.builder().auth(existingAuth).email(email).build();
    given(authSocialRepo.findFirstByEmail(email)).willReturn(Optional.of(existingSocial));

    // when
    AuthIntegrationResultDto result = authCommandService.linkOrCreateTransaction(email, password,
        Role.USER);

    // then
    assertThat(result.isNewCreated()).isFalse();
    assertThat(result.auth()).isEqualTo(existingAuth);

    verify(authRepo, never()).save(any());
    verify(authLocalRepo, times(1)).save(any(AuthLocal.class));
  }

  @Test
  @DisplayName("일반 계정 단독 저장 - Auth와 AuthLocal이 정상적으로 저장된다")
  public void saveAuth_and_saveAuthLocal_Success() {
    // given
    Auth auth = Auth.builder().role(Role.USER).build();
    String email = "test@test.com";
    String encodedPassword = "encodedPassword";

    given(authRepo.save(any(Auth.class))).willReturn(auth);
    given(authLocalRepo.existsByEmail(email)).willReturn(false);

    // when
    Auth savedAuth = authCommandService.saveAuth(Role.USER);
    authCommandService.saveAuthLocal(savedAuth, email, encodedPassword);

    // then
    verify(authRepo, times(1)).save(any(Auth.class));
    verify(authLocalRepo, times(1)).save(any(AuthLocal.class));
  }
}