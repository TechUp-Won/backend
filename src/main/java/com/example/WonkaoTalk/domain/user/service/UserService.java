package com.example.WonkaoTalk.domain.user.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import com.example.WonkaoTalk.domain.user.dto.UserSignUpRequest;
import com.example.WonkaoTalk.domain.user.dto.UserSignUpResponse;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final AuthService authService;
  private final UserRepo userRepo;

  @Transactional
  public UserSignUpResponse signUpAsUser(UserSignUpRequest request) {
    if (!request.password().equals(request.passwordCheck())) {
      throw new BusinessException(ErrorCode.AUTH_MISMATCH_PASSWORD);
    }

    Auth auth = authService.createAuthLocal(request.email(), request.password(), Role.USER);

    User user = User.builder()
        .auth(auth)
        .name(request.name())
        .nickname(request.nickname())
        .phone(request.phone())
        .birthDate(request.birthDate())
        .gender(request.gender())
        .build();

    userRepo.save(user);

    return UserSignUpResponse.of(auth, user);
  }

  @Transactional
  public void withdrawUser(Long authId) {
    User user = userRepo.findByAuthId(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    user.anonymize();

    userRepo.delete(user);
  }

  @Transactional(readOnly = true)
  public boolean existsActiveUser(Long authId) {
    return userRepo.existsByAuthId(authId);
  }


}
