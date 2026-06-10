package com.example.WonkaoTalk.domain.auth.controller;

import com.example.WonkaoTalk.common.config.OpenApiConfig;
import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckRequest;
import com.example.WonkaoTalk.domain.auth.dto.EmailCheckResponse;
import com.example.WonkaoTalk.domain.auth.dto.LoginRequest;
import com.example.WonkaoTalk.domain.auth.dto.LoginResponse;
import com.example.WonkaoTalk.domain.auth.dto.TokenDto;
import com.example.WonkaoTalk.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "이메일 중복 확인, 로그인, 로그아웃, 토큰 재발급 API")
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "이메일 중복 확인", description = "회원가입 전 이메일 사용 가능 여부를 확인합니다.")
  @PostMapping("/check-email")
  public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(
      @Valid @RequestBody EmailCheckRequest request
  ) {
    EmailCheckResponse data = authService.validateEmail(request);

    String message =
        data.isValid() ? "사용 기능한 이메일입니다." : "이미 사용 중인 이메일입니다.";

    return ResponseEntity.ok(ApiResponse.success(message, data));
  }

  @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 access token과 refresh token을 발급합니다.")
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

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "로그아웃", description = "access token을 블랙리스트에 등록하고 refresh token 쿠키를 제거합니다.")
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @RequestHeader("Authorization") String authHeader,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }

    if (userDetails == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    String accessToken = authHeader.substring(7);
    String email = userDetails.getUsername();

    authService.invalidateToken(email, accessToken);

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

  @Operation(summary = "토큰 재발급", description = "refresh token 쿠키를 검증해 새로운 access token과 refresh token을 발급합니다.")
  @PostMapping("/reissue")
  public ResponseEntity<ApiResponse<LoginResponse>> reissueToken(
      @CookieValue(value = "refresh-token", required = false) String refreshToken
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
