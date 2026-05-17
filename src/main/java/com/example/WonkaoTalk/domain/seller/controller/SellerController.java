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
public class SellerController {

  private final SellerService sellerService;
  private final AccountWithdraw accountWithdraw;

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<SellerSignUpResponse>> signUp(
      @Valid @RequestBody SellerSignUpRequest request
  ) {
    SellerSignUpResponse response = sellerService.signUpAsSeller(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("판매자 회원가입이 완료되었습니다.", response));
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<SellerSignUpResponse>> register(
      @Valid @RequestBody SellerRegisterRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    Long authId = userDetails.getAuthId();

    SellerSignUpResponse response = sellerService.registerSeller(authId, request);

    return ResponseEntity.ok(ApiResponse.success("판매자 등록이 완료되었습니다.", response));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<SellerResponse>> getMySellerInfo(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    SellerResponse response = sellerService.getSellerInfo(userDetails.getSellerId());

    return ResponseEntity.ok(ApiResponse.success("판매자 정보를 조회했습니다.", response));
  }

  @PatchMapping
  public ResponseEntity<ApiResponse<SellerResponse>> updateMyInfo(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody SellerUpdateRequest request
  ) {
    SellerResponse response = sellerService.updateSellerInfo(userDetails.getSellerId(), request);

    return ResponseEntity.ok(ApiResponse.success("판매자 정보 수정이 완료되었습니다.", response));
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

    accountWithdraw.withdrawSeller(userDetails.getAuthId(), userDetails.getUsername(), accessToken);

    return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
  }


}
