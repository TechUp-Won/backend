package com.example.WonkaoTalk.domain.auth.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckResponse;
import com.example.WonkaoTalk.domain.auth.dto.LoginRequest;
import com.example.WonkaoTalk.domain.auth.dto.LoginResponse;
import com.example.WonkaoTalk.domain.auth.dto.SignUpRequest;
import com.example.WonkaoTalk.domain.auth.dto.SignUpResponse;
import com.example.WonkaoTalk.domain.auth.dto.TokenDto;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
        data.isValid() ? "사용 기능한 이메일입니다." : "이미 사용 중인 이메일입니다.";

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

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest
  ) {
    TokenDto dto = authService.login(request, httpRequest);

    ResponseCookie refreshCookie = ResponseCookie.from("refresh-token", dto.refreshToken())
        .httpOnly(true)
        .secure(false) // TODO: 배포 시 HTTPS 환경에서는 true로 변경
        .path("/")
        .maxAge(14 * 24 * 60 * 60) //14일
        .sameSite("Strict")
        .build();

    LoginResponse responseBody = LoginResponse.of(
        dto.accessToken(),
        dto.accessExpirationTime(),
        dto.auth(),
        dto.user()
    );

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(ApiResponse.success("로그인에 성공하였습니다.", responseBody));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      HttpServletRequest request,
      Authentication authentication
  ) {
    String bearerToken = request.getHeader("Authorization");
    String accessToken = null;
    if (bearerToken != null && bearerToken.startsWith("Bearer")) {
      accessToken = bearerToken.substring(7);
    }
    String email = authentication.getName();

    authService.logout(accessToken, email);

    ResponseCookie deleteCookie = ResponseCookie.from("refresh-token", "")
        .httpOnly(true)
        .secure(false) // TODO: 배포 시 HTTPS 환경에서는 true로 변경
        .path("/")
        .maxAge(0)
        .sameSite("Strict")
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
        .body(ApiResponse.success("로그아웃에 성공하였습니다.", null));
  }

}
