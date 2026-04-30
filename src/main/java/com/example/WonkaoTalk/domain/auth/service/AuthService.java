package com.example.WonkaoTalk.domain.auth.service;

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
import com.example.WonkaoTalk.domain.auth.repo.AuthSocialRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final AuthRepo authRepo;
  private final AuthLocalRepo authLocalRepo;
  private final AuthSocialRepo authSocialRepo;
  private final UserRepo userRepo;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public EmailCheckResponse validateEmail(EmailCheckRequest request) {
    boolean exists = authLocalRepo.existsByEmail(request.email());

    return EmailCheckResponse.from(!exists);
  }

  @Transactional
  public SignUpResponse signUp(SignUpRequest request) {
    if (!request.password().equals(request.passwordCheck())) {
      throw new BusinessException(ErrorCode.AUTH_MISMATCH_PASSWORD);
    }

    if (authLocalRepo.existsByEmail(request.email())) {
      throw new BusinessException(ErrorCode.AUTH_DUPLICATE_EMAIL);
    }

    Auth auth = Auth.builder().build();
    Auth savedAuth = authRepo.save(auth);

    String encodedPassword = passwordEncoder.encode(request.password());
    AuthLocal authLocal = AuthLocal.builder()
        .auth(savedAuth)
        .email(request.email())
        .passwordHash(encodedPassword)
        .failedAttemptsCount(0)
        .build();
    authLocalRepo.save(authLocal);

    User user = User.builder()
        .authId(savedAuth)
        .name(request.name())
        .nickname(request.nickname())
        .phone(request.phone())
        .birthDate(request.birthDate())
        .gender(request.gender())
        .build();
    User savedUser = userRepo.save(user);

    return SignUpResponse.builder()
        .authId(savedAuth.getId())
        .userId(savedUser.getId())
        .createdAt(savedAuth.getCreatedAt())
        .build();
  }
}
