package com.example.WonkaoTalk.domain.seller.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.seller.dto.SellerRegisterRequest;
import com.example.WonkaoTalk.domain.seller.dto.SellerResponse;
import com.example.WonkaoTalk.domain.seller.dto.SellerSignUpRequest;
import com.example.WonkaoTalk.domain.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sellers")
public class SellerController {

  private final SellerService sellerService;

  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<SellerResponse>> signUp(
      @Valid @RequestBody SellerSignUpRequest request
  ) {
    SellerResponse response = sellerService.signUpAsSeller(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("판매자 회원가입이 완료되었습니다.", response));
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<SellerResponse>> register(
      @Valid @RequestBody SellerRegisterRequest request,
      Authentication authentication
  ) {
    Long authId = userDatails.getAuthId();

    SellerResponse response = sellerService.registerSeller(authId, request);

    return ResponseEntity.ok(ApiResponse.success("판매자 등록이 완료되었습니다.", response));
  }

}
