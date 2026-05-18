package com.example.WonkaoTalk.domain.auth.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
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

  @Transactional
  public void withdrawAuth(Long authId) {
    Auth auth = authRepo.findById(authId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_NOT_FOUND));
    authLocalRepo.findByAuth(auth).ifPresent(AuthLocal::withdraw);
    auth.withdraw();
  }

  @Transactional(readOnly = true)
  public String extractProfileNameByRole(Auth auth) {
    Role role = auth.getRole();

    if (role == Role.USER || role == Role.USER_SELLER) {
      return userRepo.findByAuth(auth).map(User::getNickname)
          .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    } else if (role == Role.SELLER) {
      return sellerRepo.findByAuth(auth).map(Seller::getName)
          .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
    } else if (role == Role.ADMIN) {
      return "관리자";
    }
    return "알 수 없는 사용자";
  }

  @Transactional(readOnly = true)
  public Long extractUserIdIfPresent(Auth auth) {
    return (auth.getRole() == Role.USER || auth.getRole() == Role.USER_SELLER)
        ? userRepo.findByAuth(auth).map(User::getId).orElse(null) : null;
  }

  @Transactional(readOnly = true)
  public Long extractSellerIdIfPresent(Auth auth) {
    return (auth.getRole() == Role.SELLER || auth.getRole() == Role.USER_SELLER)
        ? sellerRepo.findByAuth(auth).map(Seller::getId).orElse(null) : null;
  }
}
