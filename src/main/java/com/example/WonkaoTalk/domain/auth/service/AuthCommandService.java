package com.example.WonkaoTalk.domain.auth.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.domain.auth.dto.AuthUserInfoDto;
import com.example.WonkaoTalk.domain.auth.entity.Auth;
import com.example.WonkaoTalk.domain.auth.entity.AuthLocal;
import com.example.WonkaoTalk.domain.auth.entity.LoginHistory;
import com.example.WonkaoTalk.domain.auth.enums.LoginStatus;
import com.example.WonkaoTalk.domain.auth.enums.Role;
import com.example.WonkaoTalk.domain.auth.repo.AuthLocalRepo;
import com.example.WonkaoTalk.domain.auth.repo.AuthRepo;
import com.example.WonkaoTalk.domain.auth.repo.LoginHistoryRepo;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.repo.SellerRepo;
import com.example.WonkaoTalk.domain.user.entity.User;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthCommandService {

  private final AuthRepo authRepo;
  private final AuthLocalRepo authLocalRepo;
  private final LoginHistoryRepo loginHistoryRepo;
  private final UserRepo userRepo;
  private final SellerRepo sellerRepo;

  @Transactional(readOnly = true)
  public boolean existsByEmail(String email) {
    return authLocalRepo.existsByEmail(email);
  }

  @Transactional
  public Auth saveAuthLocal(String email, String encodedPassword, Role role) {
    if (authLocalRepo.existsByEmail(email)) {
      throw new BusinessException(ErrorCode.AUTH_DUPLICATE_EMAIL);
    }

    Auth auth = Auth.builder().role(role).build();
    Auth savedAuth = authRepo.save(auth);

    AuthLocal authLocal = AuthLocal.builder()
        .auth(savedAuth)
        .email(email)
        .passwordHash(encodedPassword)
        .failedAttemptsCount(0)
        .build();
    authLocalRepo.save(authLocal);

    return savedAuth;
  }

  @Transactional(readOnly = true)
  public AuthLocal getAuthLocalByEmail(String email) {
    return authLocalRepo.findByEmailWithAuth(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_EMAIL));
  }

  @Transactional
  public void saveLoginHistory(Auth auth, LoginStatus status, String userAgent, String ipAddress) {
    LoginHistory history = LoginHistory.builder()
        .auth(auth)
        .ipAddress(ipAddress)
        .userAgent(userAgent)
        .status(status)
        .build();
    loginHistoryRepo.save(history);
  }

  @Transactional(readOnly = true)
  public AuthUserInfoDto getAuthUserInfo(Auth auth) {
    Role role = auth.getRole();
    String profileName = "Unknown";
    Long userId = null;
    Long sellerId = null;
    if (role == Role.USER || role == Role.USER_SELLER) {
      User user = userRepo.findByAuth(auth)
          .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
      profileName = user.getNickname();
      userId = user.getId();
    }
    if (auth.getRole() == Role.SELLER || auth.getRole() == Role.USER_SELLER) {
      Seller seller = sellerRepo.findByAuth(auth)
          .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
      if (role == Role.SELLER) {
        profileName = seller.getName();
      }
      sellerId = seller.getId();
    }
    return AuthUserInfoDto.of(profileName, userId, sellerId);
  }

  @Transactional
  public void handleUserWithdraw(Long authId, boolean hasActiveSeller) {
    Auth auth = authRepo.findById(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_NOT_FOUND));
    if ((hasActiveSeller)) {
      auth.updateRole(Role.SELLER);
    } else {
      authLocalRepo.findByAuth(auth).ifPresent(AuthLocal::withdraw);
      auth.withdraw();
    }
  }

  @Transactional
  public void handleSellerWithdraw(Long authId, boolean hasActiveUser) {
    Auth auth = authRepo.findById(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_NOT_FOUND));
    if ((hasActiveUser)) {
      auth.updateRole(Role.USER);
    } else {
      authLocalRepo.findByAuth(auth).ifPresent(AuthLocal::withdraw);
      auth.withdraw();
    }
  }
}