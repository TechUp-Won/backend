package com.example.WonkaoTalk.domain.shipping.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressCreateRequest;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressResponse;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressUpdateRequest;
import com.example.WonkaoTalk.domain.shipping.service.ShippingAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shipping/addresses")
public class ShippingAddressController {

  private final ShippingAddressService shippingAddressService;

  @PostMapping
  public ResponseEntity<ApiResponse<ShippingAddressResponse>> create(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ShippingAddressCreateRequest request
  ) {
    ShippingAddressResponse response = shippingAddressService.create(userDetails.getUserId(),
        request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("배송지가 등록되었습니다.", response));
  }

  @PatchMapping("/{shippingAddressId}")
  public ResponseEntity<ApiResponse<ShippingAddressResponse>> update(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long shippingAddressId,
      @Valid @RequestBody ShippingAddressUpdateRequest request
  ) {
    ShippingAddressResponse response = shippingAddressService.update(userDetails.getUserId(),
        shippingAddressId, request);
    return ResponseEntity.ok(ApiResponse.success("배송지가 수정되었습니다.", response));
  }

  @DeleteMapping("/{shippingAddressId}")
  public ResponseEntity<ApiResponse<Void>> delete(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long shippingAddressId
  ) {
    shippingAddressService.delete(userDetails.getUserId(), shippingAddressId);
    return ResponseEntity.ok(ApiResponse.success("배송지가 삭제되었습니다.", null));
  }

  @PatchMapping("/{shippingAddressId}/default")
  public ResponseEntity<ApiResponse<ShippingAddressResponse>> setDefault(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long shippingAddressId
  ) {
    ShippingAddressResponse response = shippingAddressService.setDefault(userDetails.getUserId(),
        shippingAddressId);
    return ResponseEntity.ok(ApiResponse.success("기본 배송지로 설정되었습니다.", response));
  }
}
