package com.example.WonkaoTalk.application.facade;

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

  @Transactional
  public void withdrawUser(Long authId, String email, String accessToken) {
    userService.withdrawUser(authId);

    if (!sellerService.existsActiveSeller(authId)) {
      authService.withdraw(authId);
    }
    authService.invalidateToken(email, accessToken);
  }

  @Transactional
  public void withdrawSeller(Long authId, String email, String accessToken) {
    sellerService.withdrawSeller(authId);

    if (!userService.existsActiveUser(authId)) {
      authService.withdraw(authId);
    }
    authService.invalidateToken(email, accessToken);
  }
}
