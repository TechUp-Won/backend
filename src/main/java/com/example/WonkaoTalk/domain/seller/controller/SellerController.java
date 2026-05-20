package com.example.WonkaoTalk.domain.seller.controller;

import com.example.WonkaoTalk.application.facade.AccountWithdraw;
import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.seller.dto.SellerRegisterRequest;
import com.example.WonkaoTalk.domain.seller.dto.SellerResponse;
import com.example.WonkaoTalk.domain.seller.dto.SellerSignUpRequest;
import com.example.WonkaoTalk.domain.seller.dto.SellerSignUpResponse;
import com.example.WonkaoTalk.domain.seller.dto.SellerUpdateRequest;
import com.example.WonkaoTalk.domain.seller.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequiredArgsConstructor
@RequestMapping("/api/v1/sellers")
@Tag(name = "판매자", description = "판매자 회원가입, 판매자 등록, 판매자 정보 조회/수정/탈퇴 API")
public class SellerController {

  private final SellerService sellerService;
  private final AccountWithdraw accountWithdraw;

  @Operation(summary = "판매자 회원가입", description = "이메일, 비밀번호, 사업자 정보를 입력해 판매자 계정을 생성합니다.")
  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<SellerSignUpResponse>> signUp(
      @Valid @RequestBody SellerSignUpRequest request
  ) {
    SellerSignUpResponse response = sellerService.signUpAsSeller(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("판매자 회원가입이 완료되었습니다.", response));
  }

  @Operation(summary = "판매자 등록", description = "기존 계정에 판매자 정보를 추가 등록합니다.")
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<SellerSignUpResponse>> register(
      @Valid @RequestBody SellerRegisterRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    Long authId = userDetails.getAuthId();

    SellerSignUpResponse response = sellerService.registerSeller(authId, request);

    return ResponseEntity.ok(ApiResponse.success("판매자 등록이 완료되었습니다.", response));
  }

  @Operation(summary = "판매자 정보 조회", description = "로그인한 판매자의 정보를 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<SellerResponse>> getMySellerInfo(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    SellerResponse response = sellerService.getSellerInfo(userDetails.getSellerId());

    return ResponseEntity.ok(ApiResponse.success("판매자 정보를 조회했습니다.", response));
  }

  @Operation(summary = "판매자 정보 수정", description = "로그인한 판매자의 사업자 정보를 수정합니다.")
  @PatchMapping
  public ResponseEntity<ApiResponse<SellerResponse>> updateMyInfo(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody SellerUpdateRequest request
  ) {
    SellerResponse response = sellerService.updateSellerInfo(userDetails.getSellerId(), request);

    return ResponseEntity.ok(ApiResponse.success("판매자 정보 수정이 완료되었습니다.", response));
  }

  @Operation(summary = "판매자 탈퇴", description = "로그인한 판매자 계정을 탈퇴 처리하고 access token을 만료 처리합니다.")
  @DeleteMapping("/withdraw")
  public ResponseEntity<ApiResponse<Void>> withdraw(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestHeader("Authorization") String authHeader
  ) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }
    String accessToken = authHeader.substring(7);

    accountWithdraw.withdrawSeller(userDetails.getAuthId(), userDetails.getUsername(), accessToken);

    return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
  }


}
