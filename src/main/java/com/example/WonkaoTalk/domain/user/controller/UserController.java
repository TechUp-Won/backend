package com.example.WonkaoTalk.domain.user.controller;

import com.example.WonkaoTalk.application.facade.AccountWithdraw;
import com.example.WonkaoTalk.common.config.OpenApiConfig;
import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.user.dto.UserResponse;
import com.example.WonkaoTalk.domain.user.dto.UserSearchRequest;
import com.example.WonkaoTalk.domain.user.dto.UserSearchResponse;
import com.example.WonkaoTalk.domain.user.dto.UserSignUpRequest;
import com.example.WonkaoTalk.domain.user.dto.UserSignUpResponse;
import com.example.WonkaoTalk.domain.user.dto.UserUpdateRequest;
import com.example.WonkaoTalk.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "일반 사용자 회원가입, 내 정보 조회/수정/탈퇴 API")
public class UserController {

  private final UserService userService;
  private final AccountWithdraw accountWithdraw;

  @Operation(
      summary = "일반 사용자 회원가입",
      description = "이메일, 비밀번호, 이름, 닉네임, 전화번호, 생년월일, 성별 정보를 입력해 일반 사용자 계정을 생성합니다."
  )
  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<UserSignUpResponse>> signUp(
      @Valid @RequestBody UserSignUpRequest request
  ) {
    UserSignUpResponse response = userService.signUpAsUser(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("일반 회원가입이 완료되었습니다.", response));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "내 정보 조회", description = "로그인한 일반 사용자의 프로필 정보를 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    UserResponse response = userService.getUserInfo(userDetails.getUserId());

    return ResponseEntity.ok(ApiResponse.success("내 정보를 조회했습니다.", response));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "내 정보 수정", description = "로그인한 일반 사용자의 프로필 정보를 수정합니다.")
  @PatchMapping
  public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UserUpdateRequest request
  ) {
    UserResponse response = userService.updateUserInfo(userDetails.getUserId(), request);

    return ResponseEntity.ok(ApiResponse.success("내 정보 수정이 완료되었습니다.", response));
  }

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "일반 사용자 탈퇴", description = "로그인한 일반 사용자 계정을 탈퇴 처리하고 access token을 만료 처리합니다.")
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

  @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
  @Operation(summary = "전화번호로 사용자 검색", description = "전화번호로 친구 추가 대상 사용자를 검색합니다.")
  @PostMapping("/search")
  public ResponseEntity<ApiResponse<UserSearchResponse>> searchUserByPhone(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UserSearchRequest request
  ) {
    UserSearchResponse response = userService.findUserByPhone(userDetails.getUserId(),
        request.phone());

    return ResponseEntity.ok(ApiResponse.success("사용자 검색이 완료되었습니다.", response));
  }

}
