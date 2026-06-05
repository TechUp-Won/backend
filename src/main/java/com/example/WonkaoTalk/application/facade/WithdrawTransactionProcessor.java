package com.example.WonkaoTalk.application.facade;

import com.example.WonkaoTalk.domain.auth.service.AuthCommandService;
import com.example.WonkaoTalk.domain.seller.service.SellerService;
import com.example.WonkaoTalk.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WithdrawTransactionProcessor {

  private final UserService userService;
  private final SellerService sellerService;
  private final AuthCommandService authCommandService;

  @Transactional
  public void withdrawUser(Long authId) {
    userService.withdrawUser(authId);

    boolean hasActiveSeller = sellerService.existsActiveSeller(authId);
    authCommandService.handleUserWithdraw(authId, hasActiveSeller);
  }

  @Transactional
  public void withdrawSeller(Long authId) {
    sellerService.withdrawSeller(authId);

    boolean hasActiveUser = userService.existsActiveUser(authId);
    authCommandService.handleSellerWithdraw(authId, hasActiveUser);
  }
}
