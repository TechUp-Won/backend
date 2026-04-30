package com.example.WonkaoTalk.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckResponse;
import com.example.WonkaoTalk.domain.auth.dto.SignUpRequest;
import com.example.WonkaoTalk.domain.auth.dto.SignUpResponse;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.repo.AuthLocalRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @InjectMocks
  private AuthService authService;

  @Mock
  private AuthRepo authRepo;
  @Mock
  private AuthLocalRepo authLocalRepo;
  @Mock
  private UserRepo userRepo;
  @Mock
  private PasswordEncoder passwordEncoder;

  @Test
  @DisplayName("사용 가능한 이메일 중복 검사")
  public void checkEmailAvailable() {
    //given
    EmailCheckRequest request = new EmailCheckRequest("test@test.com");
    given(authLocalRepo.existsByEmail(request.email())).willReturn(false);

    //when
    EmailCheckResponse response = authService.validateEmail(request);

    //then
    assertThat(response.isValid()).isTrue();
  }

  @Test
  @DisplayName("정상적으로 회원가입에 성공")
  public void signUpSuccess() {
    //given
    SignUpRequest request = new SignUpRequest(
        "test@test.com", "Qwer1234", "Qwer1234",
        "이병건", "침착맨", "010-1234-5678",
        null, Gender.MALE
    );

    given(authLocalRepo.existsByEmail(request.email())).willReturn(false);
    given(passwordEncoder.encode(request.password())).willReturn("EncodedPassword");

    Auth auth = Auth.builder().build();
    given(authRepo.save(any(Auth.class))).willReturn(auth);

    AuthLocal authLocal = AuthLocal.builder().build();
    given(authLocalRepo.save(any(AuthLocal.class))).willReturn(authLocal);

    User user = User.builder().build();
    given(userRepo.save(any(User.class))).willReturn(user);

    //when
    SignUpResponse response = authService.signUp(request);

    //then
    assertThat(response).isNotNull();
    verify(authRepo).save(any(Auth.class));
    verify(authLocalRepo).save(any(AuthLocal.class));
    verify(userRepo).save(any(User.class));
  }

  @Test
  @DisplayName("비밀번호 확인 불일치 회원가입 실패")
  public void signUpFailedPasswordMismatch() {
    //given
    SignUpRequest request = new SignUpRequest(
        "test@test.com", "Qwer1234", "Qwer12345",
        "이병건", "침착맨", "010-1234-5678",
        null, Gender.MALE
    );

    //when & then
    BusinessException e = assertThrows(BusinessException.class, () -> {
      authService.signUp(request);
    });

    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AUTH_MISMATCH_PASSWORD);

  }

}