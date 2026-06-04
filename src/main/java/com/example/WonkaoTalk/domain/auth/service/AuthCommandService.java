package com.example.WonkaoTalk.domain.auth.service;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthCommandService {

  private final AuthRepo authRepo;
  private final AuthLocalRepo authLocalRepo;
  private final AuthSocialRepo authSocialRepo;
  private final LoginHistoryRepo loginHistoryRepo;
  private final UserRepo userRepo;
  private final SellerRepo sellerRepo;

  @Transactional(readOnly = true)
  public boolean existsByEmail(String email) {
    return authLocalRepo.existsByEmail(email);
  }

  @Transactional
  public Auth saveAuth(Role role) {
    Auth auth = Auth.builder().role(role).build();
    return authRepo.save(auth);
  }

  @Transactional
  public void saveAuthLocal(Auth auth, String email, String encodedPassword) {
    if (authLocalRepo.existsByEmail(email)) {
      throw new BusinessException(ErrorCode.AUTH_DUPLICATE_EMAIL);
    }

    AuthLocal authLocal = AuthLocal.builder()
        .auth(auth)
        .email(email)
        .passwordHash(encodedPassword)
        .failedAttemptsCount(0)
        .build();
    authLocalRepo.save(authLocal);
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
    if (hasActiveSeller) {
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
    if (hasActiveUser) {
      auth.updateRole(Role.USER);
    } else {
      authLocalRepo.findByAuth(auth).ifPresent(AuthLocal::withdraw);
      auth.withdraw();
    }
  }

  @Transactional(readOnly = true)
  public SocialLoginDto generateSocialLoginData(String email, AuthProvider provider,
      String providerId) {
    Auth auth = authSocialRepo.findByProviderAndProviderUserIdWithAuth(provider, providerId)
        .map(AuthSocial::getAuth)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_NOT_FOUND));

    Long userId = userRepo.findByAuth(auth).map(User::getId).orElse(null);
    Long sellerId = null;
    if (auth.getRole().name().contains("SELLER")) {
      sellerId = sellerRepo.findByAuth(auth).map(Seller::getId).orElse(null);
    }

    return new SocialLoginDto(auth.getId(), userId, sellerId, auth.getRole());
  }

  @Transactional(readOnly = true)
  public Optional<AuthSocial> getFirstAuthSocialByEmail(String email) {
    return authSocialRepo.findFirstByEmail(email);
  }
}