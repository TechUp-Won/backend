package com.example.WonkaoTalk.domain.auth.controller;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckResponse;
import com.example.WonkaoTalk.domain.auth.dto.LoginRequest;
import com.example.WonkaoTalk.domain.auth.dto.LoginResponse;
import com.example.WonkaoTalk.domain.auth.dto.TokenDto;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest
  ) {
    TokenDto dto = authService.login(request, httpRequest);

    ResponseCookie refreshCookie = ResponseCookie.from("refresh-token", dto.refreshToken())
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(dto.refreshExpirationTime())
        .sameSite("Strict")
        .build();

    LoginResponse responseBody = LoginResponse.of(dto.accessToken(), dto.accessExpirationTime(),
        dto.auth(), dto.profileName());

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(ApiResponse.success("로그인에 성공하였습니다.", responseBody));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @RequestHeader("Authorization") String authHeader,
      Authentication authentication
  ) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }
    String accessToken = authHeader.substring(7);
    String email = authentication.getName();

    authService.logout(accessToken, email);

    ResponseCookie deleteCookie = ResponseCookie.from("refresh-token", "")
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(0)
        .sameSite("Strict")
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
        .body(ApiResponse.success("로그아웃에 성공하였습니다.", null));
  }

  @PostMapping("/reissue")
  public ResponseEntity<ApiResponse<LoginResponse>> reissueToken(
      @CookieValue(value = "refreshToken", required = false) String refreshToken
  ) {
    if (refreshToken == null || refreshToken.isEmpty()) {
      throw new BusinessException(ErrorCode.AUTH_MISSING_TOKEN);
    }

    TokenDto dto = authService.reissueToken(refreshToken);

    ResponseCookie refreshCookie = ResponseCookie.from("refresh-token", dto.refreshToken())
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(dto.refreshExpirationTime())
        .sameSite("Strict")
        .build();

    LoginResponse response = LoginResponse.of(dto.accessToken(), dto.accessExpirationTime(),
        dto.auth(), dto.profileName());

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(ApiResponse.success("토큰 재발급에 성공하였습니다.", response));
  }

}
