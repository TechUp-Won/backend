package com.example.WonkaoTalk.domain.store.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.service.SellerService;
import com.example.WonkaoTalk.domain.store.dto.StoreCreateRequest;
import com.example.WonkaoTalk.domain.store.dto.StoreResponse;
import com.example.WonkaoTalk.domain.store.dto.StoreUpdateRequest;
import com.example.WonkaoTalk.domain.store.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreController {

  private final StoreService storeService;
  private final SellerService sellerService;

  @PostMapping
  public ResponseEntity<ApiResponse<StoreResponse>> createStore(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody StoreCreateRequest request
  ) {

    Seller seller = sellerService.findSeller(userDetails.getSellerId());
    StoreResponse response = storeService.createStore(seller, request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("스토어 등록이 완료되었습니다.", response));
  }

  @PatchMapping
  public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody StoreUpdateRequest request
  ) {
    Seller seller = sellerService.findSeller(userDetails.getSellerId());
    StoreResponse response = storeService.updateStore(seller, request);

    return ResponseEntity.ok(ApiResponse.success("스토어 정보수정이 완료되었습니다.", response));
  }

  @DeleteMapping
  public ResponseEntity<ApiResponse<StoreResponse>> deleteStore(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    Seller seller = sellerService.findSeller(userDetails.getSellerId());
    storeService.deleteStore(seller);

    return ResponseEntity.ok(ApiResponse.success("스토어 삭제가 완료되었습니다.", null));
  }
}
