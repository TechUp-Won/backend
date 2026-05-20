package com.example.WonkaoTalk.domain.store.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.seller.entity.Seller;
import com.example.WonkaoTalk.domain.seller.service.SellerService;
import com.example.WonkaoTalk.domain.store.dto.StoreCreateRequest;
import com.example.WonkaoTalk.domain.store.dto.StoreResponse;
import com.example.WonkaoTalk.domain.store.dto.StoreUpdateRequest;
import com.example.WonkaoTalk.domain.store.service.StoreService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
@Tag(name = "스토어", description = "판매자 스토어 등록, 조회, 수정, 삭제 API")
public class StoreController {

  private final StoreService storeService;
  private final SellerService sellerService;

  @Operation(summary = "스토어 등록", description = "로그인한 판매자의 스토어 정보를 등록합니다.")
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

  @Operation(summary = "스토어 조회", description = "로그인한 판매자의 스토어 정보를 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<StoreResponse>> getStore(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    Seller seller = sellerService.findSeller(userDetails.getSellerId());
    StoreResponse response = storeService.getStore(seller);

    return ResponseEntity.ok(ApiResponse.success("스토어 정보 조회가 완료되었습니다.", response));
  }

  @Operation(summary = "스토어 수정", description = "로그인한 판매자의 스토어 정보를 수정합니다.")
  @PatchMapping
  public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody StoreUpdateRequest request
  ) {
    Seller seller = sellerService.findSeller(userDetails.getSellerId());
    StoreResponse response = storeService.updateStore(seller, request);

    return ResponseEntity.ok(ApiResponse.success("스토어 정보 수정이 완료되었습니다.", response));
  }

  @Operation(summary = "스토어 삭제", description = "로그인한 판매자의 스토어를 삭제 처리합니다.")
  @DeleteMapping
  public ResponseEntity<ApiResponse<StoreResponse>> deleteStore(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    Seller seller = sellerService.findSeller(userDetails.getSellerId());
    storeService.deleteStore(seller);

    return ResponseEntity.ok(ApiResponse.success("스토어 삭제가 완료되었습니다.", null));
  }
}
