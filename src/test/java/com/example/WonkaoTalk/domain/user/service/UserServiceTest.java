package com.example.WonkaoTalk.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import com.example.WonkaoTalk.domain.user.dto.UserSignUpRequest;
import com.example.WonkaoTalk.domain.user.dto.UserSignUpResponse;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepo userRepo;
  @Mock
  private AuthService authService;

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
    User user = User.builder().id(100L).auth(auth).build();
    given(userRepo.save(any(User.class))).willReturn(user);

    //when
    UserSignUpResponse response = userService.signUpAsUser(request);

    //then
    assertThat(response).isNotNull();
    assertThat(response.authId()).isEqualTo(1L);
    verify(authService).createAuthLocal(request.email(), request.password(), Role.USER);
    verify(userRepo).save(any(User.class));
  }
}