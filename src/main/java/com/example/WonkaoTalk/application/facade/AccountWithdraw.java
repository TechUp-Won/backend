package com.example.WonkaoTalk.application.facade;

import com.example.WonkaoTalk.domain.auth.service.AuthCommandService;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import com.example.WonkaoTalk.domain.seller.service.SellerService;
import com.example.WonkaoTalk.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountWithdraw {

  private final UserService userService;
  private final SellerService sellerService;
  private final AuthService authService;
  private final AuthCommandService authCommandService;

  @Transactional
  public void withdrawUser(Long authId, String email, String accessToken) {
    userService.withdrawUser(authId);

    boolean hasActiveSeller = sellerService.existsActiveSeller(authId);
    authCommandService.handleUserWithdraw(authId, hasActiveSeller);

    authService.invalidateToken(email, accessToken);
  }

  @Transactional
  public void withdrawSeller(Long authId, String email, String accessToken) {
    sellerService.withdrawSeller(authId);

    boolean hasActiveUser = userService.existsActiveUser(authId);
    authCommandService.handleSellerWithdraw(authId, hasActiveUser);

    authService.invalidateToken(email, accessToken);
  }
}