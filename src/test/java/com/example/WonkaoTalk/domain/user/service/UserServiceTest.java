package com.example.WonkaoTalk.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.dto.AuthIntegrationResultDto;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import com.example.WonkaoTalk.domain.user.dto.UserResponse;
import com.example.WonkaoTalk.domain.user.dto.UserSearchRequest;
import com.example.WonkaoTalk.domain.user.dto.UserSearchResponse;
import com.example.WonkaoTalk.domain.user.dto.UserSignUpRequest;
import com.example.WonkaoTalk.domain.user.dto.UserSignUpResponse;
import com.example.WonkaoTalk.domain.user.dto.UserUpdateRequest;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepo userRepo;
  @Mock
  private AuthService authService;

  private User user;

  @BeforeEach
  void setUp() {
    user = User.builder()
        .nickname("침착맨")
        .name("이병건")
        .phone("010-1234-5678")
        .image("default.png")
        .gender(Gender.MALE)
        .birthDate(LocalDate.of(2001, 4, 13))
        .marketingAgree(false)
        .build();
    ReflectionTestUtils.setField(user, "id", 1L);
  }

  @Test
  @DisplayName("회원가입 - 일반 사용자 회원가입 성공")
  public void signUpSuccess() {
    //given
    UserSignUpRequest request = new UserSignUpRequest(
        "test@test.com", "Qwer1234", "Qwer1234",
        "이병건", "침착맨", "010-1234-5678",
        LocalDate.of(1983, 12, 6), Gender.MALE
    );
    Auth auth = Auth.builder().id(1L).role(Role.USER).build();
    given(authService.createAuthLocal(eq(request.email()), eq(request.password()),
        eq(Role.USER))).willReturn(auth);

    AuthIntegrationResultDto result = new AuthIntegrationResultDto(auth, true);
    given(authService.linkOrCreateLocal(request.email(), request.password(), Role.USER))
        .willReturn(result);
    User newUser = User.builder().id(100L).auth(auth).build();
    given(userRepo.save(any(User.class))).willReturn(newUser);

    //when
    UserSignUpResponse response = userService.signUpAsUser(request);

    //then
    assertThat(response).isNotNull();
    assertThat(response.authId()).isEqualTo(1L);
    assertThat(response.userId()).isEqualTo(100L);
    verify(authService, times(1)).linkOrCreateLocal(request.email(), request.password(), Role.USER);
    verify(userRepo, times(1)).save(any(User.class));
  }

  @Test
  @DisplayName("회원가입 - 기가입 전화번호로 일반 사용자 가입 시도 실패")
  void signUp_Fail_DuplicatedPhone() {
    // given
    UserSignUpRequest request = new UserSignUpRequest(
        "test@test.com", "Qwer1234", "Qwer1234",
        "이병건", "침착맨", "010-1234-5678",
        LocalDate.of(1983, 12, 6), Gender.MALE
    );
    given(userRepo.existsByPhone(request.phone())).willReturn(true);

    // when & then
    BusinessException exception = assertThrows(BusinessException.class, () ->
        userService.signUpAsUser(request)
    );
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_REGISTERED_PHONE);
    verify(authService, never()).linkOrCreateLocal(any(), any(), any());
    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  @DisplayName("정보 조회 - bearer 토큰으로 사용자 정보 조회")
  public void getMyInfoSuccess() {
    //given
    given(userRepo.findById(1L)).willReturn(Optional.of(user));
    //when
    UserResponse response = userService.getUserInfo(1L);

    //then
    assertThat(response.userId()).isEqualTo(1L);
    assertThat(response.nickname()).isEqualTo("침착맨");
    assertThat(response.name()).isEqualTo("이병건");
  }

  @Test
  @DisplayName("정보 조회 - 사용자가 DB에 존재하지 않는 경우 실패")
  public void getUserInfoFailNotFound() {
    // given
    Long userId = 1L;
    given(userRepo.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThrows(BusinessException.class, () -> userService.getUserInfo(userId));
  }

  @Test
  @DisplayName("정보 수정 - 사용자 정보 수정 성공")
  public void updateMyInfoSuccess() {
    //given
    UserUpdateRequest request = new UserUpdateRequest(
        "침병건", null, Gender.NONE, LocalDate.of(1995, 1, 1), true);
    given(userRepo.findById(1L)).willReturn(Optional.of(user));

    //when
    UserResponse response = userService.updateUserInfo(1L, request);

    //then
    assertThat(response.nickname()).isEqualTo("침병건");
    assertThat(response.gender()).isEqualTo(Gender.NONE);
    assertThat(response.birthDate()).isEqualTo(LocalDate.of(1995, 1, 1));
    assertThat(response.marketingAgree()).isTrue();
    assertThat(user.getNickname()).isEqualTo("침병건");
    assertThat(user.isMarketingAgree()).isTrue();
  }

  @Test
  @DisplayName("정보 수정 - Null 필드 방어 및 부분 업데이트")
  public void updateUserInfoSuccessPartialUpdate() {
    // given
    Long userId = 1L;
    UserUpdateRequest request = new UserUpdateRequest(
        null, null, Gender.NONE, LocalDate.of(1995, 1, 1), true);
    given(userRepo.findById(userId)).willReturn(Optional.of(user));

    // when
    userService.updateUserInfo(userId, request);

    // then
    assertThat(user.getNickname()).isEqualTo("침착맨");
    assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1995, 1, 1));
  }

  @Test
  @DisplayName("정보 수정 - 사용자가 DB에 존재하지 않는 경우 실패")
  public void updateUserInfoFailNotFound() {
    // given
    Long userId = 1L;
    UserUpdateRequest request = new UserUpdateRequest(
        "침병건", null, Gender.NONE, LocalDate.of(1995, 1, 1), true);
    given(userRepo.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThrows(BusinessException.class, () -> userService.updateUserInfo(userId, request));
  }

  @Test
  @DisplayName("사용자 검색 - 사용자 정보 검색 성공")
  public void searchUserByPhoneSuccess() {
    //given
    User targetUser = User.builder()
        .nickname("통닭천사")
        .name("이병건")
        .phone("010-2222-2222")
        .image("default.png")
        .gender(Gender.FEMALE)
        .birthDate(LocalDate.of(2001, 4, 13))
        .marketingAgree(false)
        .build();
    ReflectionTestUtils.setField(targetUser, "id", 2L);

    UserSearchRequest request = new UserSearchRequest("010-2222-2222");
    given(userRepo.findByPhone(request.phone())).willReturn(Optional.of(targetUser));

    //when
    UserSearchResponse response = userService.findUserByPhone(user.getId(), request.phone());

    //then
    assertThat(response.userId()).isEqualTo(2L);
    assertThat(response.nickname()).isEqualTo("통닭천사");
    assertThat(response.phone()).isEqualTo("010-2222-2222");
  }

  @Test
  @DisplayName("사용자 검색 - 요청자의 전화번호로 검색 시도 차단")
  public void searchUserFailSelfSearch() {
    // given
    Long currentUserId = 1L;
    User targetUser = User.builder().phone("010-1234-5678").build();
    ReflectionTestUtils.setField(targetUser, "id", 1L);

    given(userRepo.findByPhone("010-1234-5678")).willReturn(Optional.of(targetUser));

    // when & then
    assertThrows(BusinessException.class,
        () -> userService.findUserByPhone(currentUserId, "010-1234-5678"));
  }

  @Test
  @DisplayName("사용자 검색 - 존재하지 않는 전화번호 검색 시도 실패")
  public void searchUserFailNotFound() {
    // given
    Long currentUserId = 1L;
    given(userRepo.findByPhone("010-0000-0000")).willReturn(Optional.empty());

    // when & then
    assertThrows(BusinessException.class,
        () -> userService.findUserByPhone(currentUserId, "010-0000-0000"));
  }

  @Test
  @DisplayName("사용자 탈퇴 시 개인정보가 마스킹되고 Soft Delete 처리된다.")
  public void withdrawAndMaskUserSuccess() {
    // given
    User user = User.builder()
        .nickname("침착맨")
        .name("이병건")
        .phone("010-2222-2222")
        .build();
    ReflectionTestUtils.setField(user, "id", 1L);
    given(userRepo.findByAuthId(1L)).willReturn(Optional.of(user));

    // when
    userService.withdrawUser(1L);

    // then
    then(userRepo).should(times(1)).findByAuthId(1L);

    assertThat(user.getNickname()).isNotEqualTo("침착맨");
    assertThat(user.getName()).isNotEqualTo("이병건");
    assertThat(user.getPhone()).isNotEqualTo("010-2222-2222");
    assertThat(user.getDeletedAt()).isNotNull();
  }
}