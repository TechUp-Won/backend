package com.example.WonkaoTalk.domain.user.controller;

import com.example.WonkaoTalk.application.facade.AccountWithdraw;
import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.user.dto.UserSignUpRequest;
import com.example.WonkaoTalk.domain.user.dto.UserSignUpResponse;
import com.example.WonkaoTalk.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final AccountWithdraw accountWithdraw;

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<UserSignUpResponse>> signUp(
      @Valid @RequestBody UserSignUpRequest request
  ) {
    UserSignUpResponse response = userService.signUpAsUser(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("일반 회원가입이 완료되었습니다.", response));
  }

  @DeleteMapping("/withdraw")
  public ResponseEntity<ApiResponse<Void>> withdraw(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestHeader("Authorization") String authHeader
  ) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }
    String accessToken = authHeader.substring(7);

    accountWithdraw.withdrawUser(userDetails.getAuthId(), userDetails.getUsername(), accessToken);

    return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
  }

}
