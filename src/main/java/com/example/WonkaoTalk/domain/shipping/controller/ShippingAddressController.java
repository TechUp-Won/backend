package com.example.WonkaoTalk.domain.shipping.controller;

import com.example.WonkaoTalk.common.response.ApiResponse;
import com.example.WonkaoTalk.domain.auth.dto.CustomUserDetails;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressCreateRequest;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressListResponse;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressResponse;
import com.example.WonkaoTalk.domain.shipping.dto.ShippingAddressUpdateRequest;
import com.example.WonkaoTalk.domain.shipping.service.ShippingAddressService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shipping/addresses")
@Tag(name = "배송지", description = "사용자 배송지 목록 조회, 등록, 수정, 삭제, 기본 배송지 설정 API")
public class ShippingAddressController {

  private final ShippingAddressService shippingAddressService;

  @Operation(summary = "배송지 목록 조회", description = "로그인한 사용자의 저장된 배송지 목록을 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<ShippingAddressListResponse>> getList(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    ShippingAddressListResponse response = shippingAddressService.getList(userDetails.getUserId());
    return ResponseEntity.ok(ApiResponse.success("배송지 목록을 조회했습니다.", response));
  }

  @Operation(summary = "배송지 등록", description = "새 배송지를 등록합니다. 첫 배송지는 자동으로 기본 배송지로 설정됩니다.")
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

  @Operation(summary = "배송지 수정", description = "저장된 배송지의 수령인, 연락처, 주소, 메모, 기본 배송지 여부를 수정합니다.")
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

  @Operation(summary = "배송지 삭제", description = "저장된 배송지를 삭제합니다.")
  @DeleteMapping("/{shippingAddressId}")
  public ResponseEntity<ApiResponse<Void>> delete(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long shippingAddressId
  ) {
    shippingAddressService.delete(userDetails.getUserId(), shippingAddressId);
    return ResponseEntity.ok(ApiResponse.success("배송지가 삭제되었습니다.", null));
  }

  @Operation(summary = "기본 배송지 설정", description = "선택한 배송지를 기본 배송지로 설정하고 기존 기본 배송지를 해제합니다.")
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
