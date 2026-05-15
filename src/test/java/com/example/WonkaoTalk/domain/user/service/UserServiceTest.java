package com.example.WonkaoTalk.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import com.example.WonkaoTalk.domain.user.dto.UserResponse;
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
  @DisplayName("정상적으로 일반 사용자 회원가입에 성공")
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
    User newUser = User.builder().id(100L).auth(auth).build();
    given(userRepo.save(any(User.class))).willReturn(newUser);

    //when
    UserSignUpResponse response = userService.signUpAsUser(request);

    //then
    assertThat(response).isNotNull();
    assertThat(response.authId()).isEqualTo(1L);
    verify(authService).createAuthLocal(request.email(), request.password(), Role.USER);
    verify(userRepo).save(any(User.class));
  }

  @Test
  @DisplayName("사용자 정보 조회 성공")
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
  @DisplayName("사용자 정보 수정 성공")
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
}