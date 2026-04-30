package com.example.WonkaoTalk.domain.auth.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckResponse;
import com.example.WonkaoTalk.domain.auth.dto.SignUpRequest;
import com.example.WonkaoTalk.domain.auth.dto.SignUpResponse;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/check-email")
  public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(
      @Valid @RequestBody EmailCheckRequest request
  ) {
    EmailCheckResponse data = authService.validateEmail(request);

    String message =
        data.isValid() ? "시용 기능한 이메일입니다." : "이미 사용 중인 이메일입니다.";

    return ResponseEntity.ok(ApiResponse.success(message, data));
  }

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<SignUpResponse>> signUp(
      @Valid @RequestBody SignUpRequest request
  ) {
    SignUpResponse data = authService.signUp(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("회원가입이 완료되었습니다.", data));
  }
}
